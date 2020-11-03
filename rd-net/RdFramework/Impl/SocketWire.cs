using System;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Timers;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Serialization;
using JetBrains.Threading;
using Timer = System.Timers.Timer;

namespace JetBrains.Rd.Impl
{
  public static class SocketWire
  {
    private static readonly ILog ourStaticLog = Log.GetLog<Base>();
    
    public abstract class Base : WireBase
    {
      /// <summary>
      /// Timeout for <see cref="System.Net.Sockets.Socket.Connect(System.Net.EndPoint)"/>  and for <see cref="System.Net.Sockets.Socket.Receive(byte[],int,System.Net.Sockets.SocketFlags)"/>  from socket (to guarantee read_thread termination if <see cref="System.Net.Sockets.Socket.Close()"/> doesn't
      /// lead to exception thrown by <see cref="System.Net.Sockets.Socket.Receive(byte[],int,System.Net.Sockets.SocketFlags)"/> 
      /// </summary>
      public static int TimeoutMs = 500;

      private const int ACK_MSG_LEN = -1;
      private const int PING_LEN = -2;
      
      /// <summary>
      /// For logging
      /// </summary>
      public readonly string Id;
      
      protected readonly ILog Log;
      
      /// <summary>
      /// Lifetime of this wire. If counterpart disconnects, lifetime is not terminate automatically.
      /// </summary>
      private readonly Lifetime myLifetime;
      
      
      //All operations must be bound to socket (connect or accept) thread.
      protected readonly IViewableProperty<Socket> SocketProvider = new ViewableProperty<Socket> ();
      
      public readonly IViewableProperty<bool> Connected = new ViewableProperty<bool> { Value = false };
      public readonly IViewableProperty<bool> HeartbeatAlive = new ViewableProperty<bool> { Value = false };

      protected readonly ByteBufferAsyncProcessor SendBuffer;
      protected readonly object Lock = new object();
      
      public Socket Socket { get; protected set; }

      [PublicAPI]
      public long ReadBytesCount;
      
      [PublicAPI]
      public long WrittenBytesCount;
      
      private readonly Actor<long> myAcktor;
      const string DisconnectedPauseReason = "Disconnected";

      protected Base(string id, Lifetime lifetime, [NotNull] IScheduler scheduler) : base(scheduler)
      {
        Id = id;
        Log = Diagnostics.Log.GetLog(GetType());
        myLifetime = lifetime;
        myAcktor = new Actor<long>(id+"-ACK", lifetime, SendAck);

        SendBuffer = new ByteBufferAsyncProcessor(id+"-Sender", Send0);
        SendBuffer.Pause(DisconnectedPauseReason);
        SendBuffer.Start();
        
        Connected.Advise(lifetime, value => HeartbeatAlive.Value = value);
        
        
        //when connected
        SocketProvider.Advise(lifetime, socket =>
        {
//          //todo hack for multiconnection, bring it to API
//          if (SupportsReconnect) SendBuffer.Clear();

          var timer = StartHeartbeat();

          SendBuffer.ReprocessUnacknowledged();
          SendBuffer.Resume(DisconnectedPauseReason);
          
          scheduler.Queue(() => { Connected.Value = true; });                              

          try
          {
            //use current thread for receiving procedure
            ReceiverProc(socket);

          }
          finally
          {
            scheduler.Queue(() => {Connected.Value = false;});

            SendBuffer.Pause(DisconnectedPauseReason);
            
            timer.Dispose();
            
            CloseSocket(socket);
          }          
        });
      }

      private static bool ConnectionEstablished(int timeStamp, int notionTimestamp) => timeStamp - notionTimestamp <= MaximumHeartbeatDelay;

      private Timer StartHeartbeat()
      {
        void OnTimedEvent(object sender, ElapsedEventArgs e)
        {
          Ping();
        }

        var timer = new Timer(HeartBeatInterval.TotalMilliseconds){AutoReset = true};
        timer.Elapsed += OnTimedEvent;
        timer.Enabled = true;
        timer.Start();
        return timer;
      }


      public static void CloseSocket([CanBeNull] Socket socket)
      {
        if (socket == null)
          return;
        
        ourStaticLog.CatchAndDrop(() => socket.Shutdown(SocketShutdown.Both));
        
        //on netcore you can't solely execute Close() - it will hang forever
        //sometimes on netcoreapp2.1 it could hang forever during <c>Accept()</c> on other thread: https://github.com/dotnet/corefx/issues/26034 
        //we use zero timeout here to avoid blocking mode with (possible infinite) SpinWait
        // According to reference source, non-zero timeouts (infinite -1 or positive numbers) lead to the problematic code with hanging spin wait.
        // The linked corefx issue gives mixed feedback on when it's fixed (netcore 3/net 5), but we have first-hand evidence for issues on netcore 3.
        // Additionally, the worst thing that Close(0) does is sending a connection reset, which seems to be fine for our purposes.
        ourStaticLog.CatchAndDrop(() => socket.Close(0));
      }


      

      private BufferWindow myMsgLengthBuffer;
      private BufferWindow myPkg;
      private BufferWindow myPkgBuffer;
      private BufferWindow myPkgHeaderBuffer;
      private BufferWindow mySocketBuffer;
      
      
      private void ReceiverProc(Socket socket)
      {
        myPkg = new BufferWindow(16384);
        myPkgBuffer = new BufferWindow(16384);
        mySocketBuffer = new BufferWindow(16384);
        myMsgLengthBuffer = new BufferWindow(4);
        myPkgHeaderBuffer = new BufferWindow(12);
        
        while (myLifetime.IsAlive)
        {
          if (!socket.Connected)
          {
            Log.Verbose("Stop receive messages because socket disconnected");
            break;
          }
          try
          {
            if (!ReadMsg())
            {
              Log.Verbose("{0} Connection was gracefully shutdown", Id);
              break;
            }
          }
          catch (Exception e)
          {
            if (e is SocketException socketEx)
            {
              var errcode = socketEx.SocketErrorCode;
              if (errcode == SocketError.TimedOut || errcode == SocketError.WouldBlock) continue; //expected
            }


            if (e is SocketException || e is ObjectDisposedException)
            {
              Log.Verbose("Exception in SocketWire.Receive: {0} {1} {2}", e.GetType().Name, Id, e.Message);

              //That's why we don't use Timeout any more. Exception happens only on windows but blocks socket completely.
              if (e.Message.ToLower().Contains("Overlapped I/O Operation is in progress".ToLower()))
              {
                Log.Error(
                  "ERROR! Socket {0} {1} is in invalid state. Probably no more messages will be received. Exception message: '{2}'. " +
                  "Sometimes it happens because of Timeout property on socket. Your os: {3}.",
                  e.GetType().Name, Id, e.Message, Environment.OSVersion.VersionString);
              }
              
            }
            else
            {
              Log.Error(e);
            }
            break;
          }
        }
        LogTraffic();
      }

      private bool ReadMsg()
      {
        long maxSeqnAtStart = myMaxReceivedSeqn; 
        
        myMsgLengthBuffer.Lo = myMsgLengthBuffer.Hi = 0;
        if (!myMsgLengthBuffer.Read(ref myPkgBuffer, ReceiveFromPkgBuffer))
          return false;

        Int32 len = UnsafeReader.ReadInt32FromBytes(myMsgLengthBuffer.Data);
        var msgBuffer = new BufferWindow(len);

        if (!msgBuffer.Read(ref myPkgBuffer, ReceiveFromPkgBuffer))
        {
          Log.Warn("{0}: Can't read message with len={1} from the wire because connection was shut down", Id, len);
          return false;
        }
        
        if (myMaxReceivedSeqn > maxSeqnAtStart)
          myAcktor.SendAsync(myMaxReceivedSeqn);
        
        Receive(msgBuffer.Data);
        ReadBytesCount += len + sizeof(Int32 /*len*/);
        return true;
      }

      
      private int ReceiveFromPkgBuffer(byte[] buffer, int offset, int size)
      {
        //size > 0
        
        if (myPkg.Available > 0)
        {
          var sizeToCopy = Math.Min(size, myPkg.Available);
          myPkg.MoveTo(buffer, offset, sizeToCopy);
          return sizeToCopy;
        }
        else
        {
          while (true)
          {
            myPkgHeaderBuffer.Clear();
            if (!myPkgHeaderBuffer.Read(ref mySocketBuffer, ReceiveFromSocket))
              return 0;

            Int32 len = UnsafeReader.ReadInt32FromBytes(myPkgHeaderBuffer.Data);

            if (len == PING_LEN)
            {
              Int32 receivedTimestamp = UnsafeReader.ReadInt32FromBytes(myPkgHeaderBuffer.Data, sizeof(Int32));
              Int32 receivedCounterpartTimestamp = UnsafeReader.ReadInt32FromBytes(myPkgHeaderBuffer.Data, sizeof(Int32) + sizeof(Int32));
              myCounterpartTimestamp = receivedTimestamp;
              myCounterpartNotionTimestamp = receivedCounterpartTimestamp;
              
              if (ConnectionEstablished(myCurrentTimeStamp, myCounterpartNotionTimestamp))
              {
                if (!HeartbeatAlive.Value) // only on change
                {
                  Log.Trace()?.Log($"Connection is alive after receiving PING {Id}: " +
                                   $"receivedTimestamp: {receivedTimestamp}, " +
                                   $"receivedCounterpartTimestamp: {receivedCounterpartTimestamp}, " +
                                   $"currentTimeStamp: {myCurrentTimeStamp}, " +
                                   $"counterpartTimestamp: {myCounterpartTimestamp}, " +
                                   $"counterpartNotionTimestamp: {myCounterpartNotionTimestamp}");
                }
                HeartbeatAlive.Value = true;
              }
              
              continue;
            }

            Int64 seqN = UnsafeReader.ReadInt64FromBytes(myPkgHeaderBuffer.Data, sizeof(Int32));
            if (len == ACK_MSG_LEN)
            {
              SendBuffer.Acknowledge(seqN);
            }
            else
            {
              
              myPkg.Clear();
              if (!myPkg.Read(ref mySocketBuffer, ReceiveFromSocket, len))
                return 0;

              if (seqN > myMaxReceivedSeqn || seqN == 1 /*TODO new client, possible duplicate problem if ack for seqN=1 from previous client's connection hasn't passed*/)
              {
                myMaxReceivedSeqn = seqN; //will be acknowledged when we read whole message
                Assertion.Assert(myPkg.Available > 0, "myPkgBuffer.Available > 0");
                
                var sizeToCopy = Math.Min(size, myPkg.Available);
                myPkg.MoveTo(buffer, offset, sizeToCopy);
                return sizeToCopy;
              }
              else
                myAcktor.SendAsync(seqN);
            }
          }
        }
      }

      private void SendAck(long seqN)
      {
        try
        {
          using (var cookie = UnsafeWriter.NewThreadLocalWriter())
          {
            cookie.Writer.Write(ACK_MSG_LEN);
            cookie.Writer.Write(seqN);
            cookie.CopyTo(myAckPkgHeader);
          }

          lock (mySocketSendLock)
            Socket.Send(myAckPkgHeader);
        }
        catch (ObjectDisposedException)
        {
          Log.Verbose($"{Id}: Socket was disposed during ACK, seqn = {seqN}");
        }
        catch (SocketException e)
        {
          // looks like this does not deserve a warn, as the only thing that can happen is a fatal socket failure anyway, and that will likely be reported properly from other threads
          Log.Verbose(e, $"{Id}: ${e.GetType()} raised during ACK, seqn = {seqN}");
        }
        catch (Exception e)
        {
          Log.Warn(e, $"{Id}: {e.GetType()} raised during ACK, seqn = {seqN}");
        }
      }
      
      private void Ping()
      {
        if (BackwardsCompatibleWireFormat) return;
        
        try
        {
          if (!ConnectionEstablished(myCurrentTimeStamp, myCounterpartNotionTimestamp))
          {
            if (HeartbeatAlive.Value) // log only on change
            {
              Log.Trace()?.Log($"Disconnect detected while sending PING {Id}: " +
                               $"currentTimeStamp: {myCurrentTimeStamp}, " +
                               $"counterpartTimestamp: {myCounterpartTimestamp}, " +
                               $"counterpartNotionTimestamp: {myCounterpartNotionTimestamp}");
            }
            HeartbeatAlive.Value = false;
          }

          using (var cookie = UnsafeWriter.NewThreadLocalWriter())
          {
            cookie.Writer.Write(PING_LEN);
            cookie.Writer.Write(myCurrentTimeStamp);
            cookie.Writer.Write(myCounterpartTimestamp);
            cookie.CopyTo(myPingPkgHeader);
          }

          lock (mySocketSendLock)
            Socket.Send(myPingPkgHeader);

          ++myCurrentTimeStamp;
        }
        catch (ObjectDisposedException)
        {
          Log.Verbose($"{Id}: Socket was disposed during PING");
        }
        catch (Exception e)
        {
          Log.Warn(e, $"{Id}: {e.GetType()} raised during PING");
        }
      }
      
      private readonly object mySocketSendLock = new object();
      
      private int ReceiveFromSocket(byte[] buffer, int offset, int size)
      {
        return Socket.Receive(buffer, offset, size, 0);
      }

      private long mySentSeqn;
      private long myMaxReceivedSeqn;

      private const int PkgHeaderLen = sizeof(int) /*pkgFullLen */ + sizeof(long) /*seqN*/;
      private readonly byte[] mySendPkgHeader = new byte[ PkgHeaderLen];
      private readonly byte[] myAckPkgHeader = new byte[ PkgHeaderLen]; //different threads

      public TimeSpan HeartBeatInterval { get; set; } = TimeSpan.FromMilliseconds(500);
      
      /// <summary>
      /// Timestamp of this wire which increases at intervals of <see cref="HeartBeatInterval"/>
      /// </summary>
      private int myCurrentTimeStamp;
      
      /// <summary>
      /// Actual notion about counterpart's <see cref="myCurrentTimeStamp"/>
      /// </summary>
      private int myCounterpartTimestamp;
      
      /// <summary>
      /// The latest received counterpart's notion of this wire's <see cref="myCurrentTimeStamp"/>
      /// </summary>
      private int myCounterpartNotionTimestamp;
      
      internal const int MaximumHeartbeatDelay = 3;
      private readonly byte[] myPingPkgHeader = new byte[ PkgHeaderLen];

      private void Send0(byte[] data, int offset, int len, ref long seqN)
      {
        try
        {
          if (seqN == 0)
            seqN = ++mySentSeqn;

          using (var cookie = UnsafeWriter.NewThreadLocalWriter())
          {
            cookie.Writer.Write(len);
            cookie.Writer.Write(seqN);
            cookie.CopyTo(mySendPkgHeader);
          }

          lock (mySocketSendLock)
          {
            Socket.Send(mySendPkgHeader, 0, PkgHeaderLen, SocketFlags.None);
            Socket.Send(data, offset, len, SocketFlags.None);
          }
          WrittenBytesCount += len;
        }
        catch (Exception e)
        {
          if (e is SocketException || e is ObjectDisposedException)
          {
            SendBuffer.Pause(DisconnectedPauseReason);
            LogTraffic();
          }
          else
          {
            Log.Error(e);
          }
                    
        }        
      }

      protected override void SendPkg(UnsafeWriter.Cookie cookie)
      {
        SendBuffer.Put(cookie);
      }
      
      
      //It's a kind of magic...
      protected static void SetSocketOptions(Socket s)
      {
        s.NoDelay = true;              

//        if (!TimeoutForbidden())
//          s.ReceiveTimeout = TimeoutMs; //sometimes shutdown and close doesn't lead Receive to throw exception 

        //following optimization is under Windows only
//        if (!PlatformUtil.IsRunningUnderWindows) return;
//
//        const int sioLoopbackFastPath = -1744830448;
//        var optionInValue = BitConverter.GetBytes(1);
//
//        s.IOControl(
//          sioLoopbackFastPath,
//          optionInValue,
//          null);

      }

      private void LogTraffic()
      {
        Log.Verbose("{0}: Total traffic: sent {1}, received {2}", Id, WrittenBytesCount, ReadBytesCount);
      }


      //can't take socket from mySocketProvider: it could be not set yet 
      protected void AddTerminationActions([NotNull] Thread receiverThread)
      {
        // ReSharper disable once ImpureMethodCallOnReadonlyValueField
        myLifetime.OnTermination(() =>
          {
            Log.Verbose("{0}: start termination of lifetime", Id);

            var sendBufferStopped = SendBuffer.Stop(5_000);
            Log.Verbose("{0}: send buffer stopped, success: {1}", Id, sendBufferStopped);

            lock (Lock)
            {
              Log.Verbose("{0}: closing socket because of lifetime", Id);
              CloseSocket(Socket);
              Monitor.PulseAll(Lock);
            }

            Log.Verbose("{0}: waiting for receiver thread", Id);
            if (!receiverThread.Join(TimeoutMs + 100))
              Log.Verbose("{0}: unable to join receiver thread", Id);

            Log.Verbose("{0}: termination finished", Id);
          }
        );
      }

      public int Port { get; protected set; }


      protected virtual bool AcceptHandshake(Socket socket)
      {
        return true;
      }

    }

    public class Client : Base
    {
      public Client(Lifetime lifetime, [NotNull] IScheduler scheduler, int port, string optId = null) : 
        this(lifetime, scheduler, new IPEndPoint(IPAddress.Loopback, port), optId) {}

      public Client(Lifetime lifetime, [NotNull] IScheduler scheduler, [NotNull] IPEndPoint endPoint, string optId = null) : 
        base("ClientSocket-"+(optId ?? "<noname>"), lifetime, scheduler)
      {
        var thread = new Thread(() =>
        {
          try
          {
            Log.Verbose("{0} : started", Id);

            while (lifetime.IsAlive)
            {
              try
              {
                var s = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
                Socket = s;

                SetSocketOptions(s);
                Log.Verbose("{0} : connecting", Id);
                s.Connect(endPoint);

                lock (Lock)
                {
                  if (!lifetime.IsAlive)
                  {
                    Log.Verbose("{0} : connected, but lifetime is already canceled, closing socket", Id);
                    CloseSocket(s); //to guarantee socket termination
                    return;
                  }
                  else
                  {
                    Log.Verbose("{0} : connected", Id);
                  }
                }

                SocketProvider.Value = Socket;
              }

              catch (SocketException)
              {
                lock (Lock)
                {
                  if (!lifetime.IsAlive) break;
                  Monitor.Wait(Lock, TimeoutMs);
                  if (!lifetime.IsAlive) break;
                }
              }
            }


          }
          catch (SocketException e)
          {
            Log.Verbose("{0}: SocketException with message {1}", Id, e.Message);
          }
          catch (ObjectDisposedException e)
          {
            Log.Verbose("{0}: ObjectDisposedException with message {1}", Id, e.Message);
          }
          catch (Exception e)
          {
            Log.Error(e, Id);
          }
        }) {Name = Id+"-Receiver", IsBackground = true};

        thread.Start();

        AddTerminationActions(thread);
      }
    }

    public class Server : Base
    {
      public Server(Lifetime lifetime, [NotNull] IScheduler scheduler, [CanBeNull] IPEndPoint endPoint = null, string optId = null) : this(lifetime, scheduler, optId)
      {
        var serverSocket = CreateServerSocket(endPoint);

        StartServerSocket(lifetime, serverSocket);

        lifetime.OnTermination(() =>
          {
            ourStaticLog.Verbose("closing server socket");
            CloseSocket(serverSocket);
          }
        );
      }

      /// <summary>
      /// Creates a server wire with an externally-provided socket. By using this constructor, you are not transferring
      /// ownership of the provided socket to created wire. It is consumer's responsibility to manager socket's lifetime.
      /// </summary>
      public Server(Lifetime lifetime, IScheduler scheduler, Socket serverSocket, string optId = null) : this(lifetime, scheduler, optId)
      {
        StartServerSocket(lifetime, serverSocket);
      }

      private Server(Lifetime lifetime, IScheduler scheduler, string optId = null) : base("ServerSocket-"+(optId ?? "<noname>"), lifetime, scheduler)
      {}

      public static Socket CreateServerSocket([CanBeNull] IPEndPoint endPoint)
      {
        Protocol.InitLogger.Verbose("Creating server socket on endpoint: {0}", endPoint);

        var serverSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
        SetSocketOptions(serverSocket);

        endPoint = endPoint ?? new IPEndPoint(IPAddress.Loopback, 0);
        serverSocket.Bind(endPoint);
        serverSocket.Listen(1);
        Protocol.InitLogger.Verbose("Server socket created, listening started on endpoint: {0}", endPoint);

        return serverSocket;
      }

      private void StartServerSocket(Lifetime lifetime, [NotNull] Socket serverSocket)
      {
        if (serverSocket == null) throw new ArgumentNullException(nameof(serverSocket));
        Port = ((IPEndPoint) serverSocket.LocalEndPoint).Port;
        Log.Verbose("{0} : started, port: {1}", Id, Port);

        var thread = new Thread(() =>
        {

          while (lifetime.IsAlive)
          {
            try
            {
              Log.Verbose("{0} : accepting, port: {1}", Id, Port);
              var s = serverSocket.Accept();
              lock (Lock)
              {
                if (!lifetime.IsAlive)
                {
                  Log.Verbose("{0} : connected, but lifetime is already canceled, closing socket", Id);
                  CloseSocket(s);
                  return;
                }
                else
                {
                  Log.Verbose("{0} : accepted", Id);
                  if (!AcceptHandshake(s))
                    continue;
                  Socket = s;
                  Log.Verbose("{0} : connected", Id);
                }
              }

              SocketProvider.Value = s;
            }
            catch (SocketException e)
            {
              var errcode = e.SocketErrorCode;
              if (errcode == SocketError.TimedOut || errcode == SocketError.WouldBlock) continue; //expected, Linux

              Log.Verbose("{0}: SocketException with message {1}", Id, e.Message);
            }
            catch (ObjectDisposedException e)
            {
              Log.Verbose("{0}: ObjectDisposedException with message {1}", Id, e.Message);
            }
            catch (Exception e)
            {
              Log.Error(e, Id);
            }
          }
        }) {Name = Id + "-Receiver", IsBackground = true};

        thread.Start();

        AddTerminationActions(thread);
      }
    }
    
    
    public struct WireParameters
    {
      public readonly IScheduler Scheduler;
      public readonly string Id;

      public WireParameters([NotNull] IScheduler scheduler, [CanBeNull] string id)
      {
        Scheduler = scheduler ?? throw new ArgumentNullException(nameof(scheduler));
        Id = id;
      }

      public void Deconstruct(out IScheduler scheduler, out string id)
      {
        scheduler = Scheduler;
        id = Id;
      }
    }

    

    
    public class ServerFactory
    {
      [PublicAPI] public readonly int LocalPort;
      [PublicAPI] public readonly IViewableSet<Server> Connected = new ViewableSet<Server>();

      
      public ServerFactory(Lifetime lifetime, IScheduler scheduler, IPEndPoint endpoint = null) 
        : this(lifetime, () => new WireParameters(scheduler, null), endpoint) {}


      public ServerFactory(
        Lifetime lifetime,
        [NotNull] Func<WireParameters> wireParametersFactory,
        IPEndPoint endpoint = null
      )
      {
        var serverSocket = Server.CreateServerSocket(endpoint);
        var serverSocketLifetimeDef = new LifetimeDefinition(lifetime);
        serverSocketLifetimeDef.Lifetime.OnTermination(() =>
        {
          ourStaticLog.Verbose("closing server socket");
          Base.CloseSocket(serverSocket);
        });
        LocalPort = ((IPEndPoint) serverSocket.LocalEndPoint).Port; 
        
        void Rec()
        {
          lifetime.TryExecute(() =>
          {
            var (scheduler, id) = wireParametersFactory();
            var s = new Server(lifetime, scheduler, serverSocket, id);
            // Each server will spawn a thread that will be waiting in serverSocket.Accept method. When lifetime
            // termination is invoked, these threads synchronously join the termination thread. Since these Thread.Join
            // calls are located deeper in the Lifetime termination stack we have to place this socket termination call
            // after each server creation.
            lifetime.OnTermination(() => serverSocketLifetimeDef.Terminate());

            s.Connected.WhenTrue(lifetime, lt =>
            {
              Connected.AddLifetimed(lt, s);
              Rec();
            });
          });
        }



        Rec();
      }
      
    }
  }
}
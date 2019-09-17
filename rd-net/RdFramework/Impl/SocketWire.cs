using System;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Serialization;
using JetBrains.Threading;
using JetBrains.Util;

namespace JetBrains.Rd.Impl
{
  public static class SocketWire
  {    
    public abstract class Base : WireBase
    {
      /// <summary>
      /// Timeout for <see cref="System.Net.Sockets.Socket.Connect(System.Net.EndPoint)"/>  and for <see cref="System.Net.Sockets.Socket.Receive(byte[],int,System.Net.Sockets.SocketFlags)"/>  from socket (to guarantee read_thread termination if <see cref="System.Net.Sockets.Socket.Close()"/> doesn't
      /// lead to exception thrown by <see cref="System.Net.Sockets.Socket.Receive(byte[],int,System.Net.Sockets.SocketFlags)"/> 
      /// </summary>
      protected const int TimeoutMs = 500;

      private const int ACK_MSG_LEN = -1;
      
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
        myAcktor = new Actor<long>(id+"-ACK", lifetime, (Action<long>)SendAck);

        SendBuffer = new ByteBufferAsyncProcessor(id+"-Sender", Send0);
        SendBuffer.Pause(DisconnectedPauseReason);
        SendBuffer.Start();
        
        
        
        //when connected
        SocketProvider.Advise(lifetime, socket =>
        {
//          //todo hack for multiconnection, bring it to API
//          if (SupportsReconnect) SendBuffer.Clear();
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
            
            CloseSocket(socket);
          }          
        });
      }

      protected void CloseSocket([CanBeNull] Socket socket)
      {
        if (socket == null)
        {
          Log.Verbose("{0}: socket is null", Id);
          return;
        }
        
        Log.CatchAndDrop(() => socket.Shutdown(SocketShutdown.Both));
        Log.CatchAndDrop(socket.Close);
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
                  "Sometimes it happens on Windows 8. Your os: {3}." +
                  "In case this problem arises ask Ivan Paschenko.",
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
            Int64 seqN = UnsafeReader.ReadInt64FromBytes(myPkgHeaderBuffer.Data, sizeof(Int32));

            if (len == ACK_MSG_LEN) 
              SendBuffer.Acknowledge(seqN);
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
        catch (Exception e)
        {
          Log.Warn(e, $"{Id}: Exception raised during ACK, seqn = {seqN}");
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


      //can't take socket from mySoсketProvider: it could be not set yet 
      protected void AddTerminationActions([NotNull] Thread receiverThread)
      {
        // ReSharper disable once ImpureMethodCallOnReadonlyValueField
        myLifetime.OnTermination(() =>
          {
            Log.Verbose("{0}: start termination of lifetime", Id);

            var sendBufferStopped = SendBuffer.Stop(5000);
            Log.Verbose("{0}: send buffer stopped, success: {1}", Id, sendBufferStopped);

            lock (Lock)
            {
              Log.Verbose("{0}: closing socket because of lifetime", Id);
              CloseSocket(Socket);
              Monitor.PulseAll(Lock);
            }

            Log.Verbose("{0}: waiting for receiver thread", Id);
            receiverThread.Join(TimeoutMs + 100);
            Log.Verbose("{0}: termination finished", Id);
          }
        );
      }

      protected void StartServerSocket(Lifetime lifetime, [CanBeNull] IPEndPoint endPoint)
      {
        Log.Verbose("{0} : started", Id);

        Socket serverSocket = null;
        var serverSocketReady = new ManualResetEvent(false);

        var thread = new Thread(() =>
        {
          try
          {
            serverSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            SetSocketOptions(serverSocket);
            serverSocket.Bind(endPoint ?? new IPEndPoint(IPAddress.Loopback, 0));

            if (serverSocket.LocalEndPoint is IPEndPoint ipEndPoint)
              Port = ipEndPoint.Port;

            serverSocket.Listen(1);
            Log.Verbose("{0} : listening", Id);
          }
          catch (Exception e)
          {
            Log.Error(Id + " : Failed to listen server socket", e);
            return;
          }
          finally
          {
            serverSocketReady.Set();
          }

          while (lifetime.IsAlive)
          {
            try
            {
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


        serverSocketReady.WaitOne();
        Log.Verbose("{0}: finished synchronous wait for server socket listening, port={1}, serverSocket initialized: {2}", Id, Port,
          serverSocket != null);
        if (serverSocket == null) return;


        AddTerminationActions(thread);

        //order is critical: we need to close server socket first and the terminate other stuff and wait for thread 
        lifetime.OnTermination(() =>
        {
          Log.Verbose("{0}: closing server socket", Id);

          CloseSocket(serverSocket);
        });
      }

      public int Port { get; private set; }


      protected virtual bool AcceptHandshake(Socket socket)
      {
        return true;
      }

    }

    public class Client : Base
    {
      public Client(Lifetime lifetime, [NotNull] IScheduler scheduler, int port, string optId = null) : this(lifetime, scheduler, new IPEndPoint(IPAddress.Loopback, port), optId) {}

      public Client(Lifetime lifetime, [NotNull] IScheduler scheduler, [NotNull] IPEndPoint endPoint, string optId = null) : base("ClientSocket-"+(optId ?? "<noname>"), lifetime, scheduler)
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
                    Socket = s;
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
      public Server(Lifetime lifetime, [NotNull] IScheduler scheduler, [CanBeNull] IPEndPoint endPoint = null, string optId = null)
        : base("ServerSocket-"+(optId ?? "<noname>"), lifetime, scheduler)
      {
        
        StartServerSocket(lifetime, endPoint);
      }
    }
  }
}
using System;
using System.Threading;
using JetBrains.Annotations;

namespace JetBrains.Rd
{
    /// <summary>
    /// ClientId is a global context class that is used to distinguish the originator of an action in multi-client systems
    /// In such systems, each client has their own ClientId.
    /// 
    /// The context is automatically propagated across async/await calls using AsyncLocal. The application should take care to preserve and propagate the current value across other kinds of asynchronous calls. 
    /// </summary>
    public struct ClientId
    {
        public readonly string Value;

        public ClientId(string value)
        {
            Value = value;
        }

        public override string ToString()
        {
            return Value;
        }

        public const string LocalId = "Host";

#if !NET35
        private static readonly AsyncLocal<ClientId?> ourAsyncLocalClientId = new AsyncLocal<ClientId?>();
#endif
        
        
        public static readonly CtxReadDelegate<ClientId> ReadDelegate = (ctx, reader) => new ClientId(reader.ReadString());
        public static readonly CtxWriteDelegate<ClientId> WriteDelegate = (ctx, writer, value) => writer.Write(value.Value);


        #region Cookie

        public struct ClientIdCookie : IDisposable
        {
            private readonly ClientId? myOldClientId;

            public ClientIdCookie(ClientId newClientId)
            {
                myOldClientId = CurrentOrNull;
                SetClientId(newClientId);
            }

            private static void SetClientId(ClientId? newClientId)
            {
#if !NET35
                ourAsyncLocalClientId.Value = newClientId;
#endif
            }

            public void Dispose()
            {
                SetClientId(myOldClientId);
            }
        }

        #endregion

        public static ClientIdCookie CreateCookie(ClientId clientId) => new ClientIdCookie(clientId);

        [CanBeNull]
        public static ClientId? CurrentOrNull =>
#if !NET35
            ourAsyncLocalClientId.Value;
#else
            throw new NotSupportedException("No ClientId on NET 3.5");
#endif
    }
}
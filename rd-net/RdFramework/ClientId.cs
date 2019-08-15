using System;
using System.Threading;
using JetBrains.Annotations;

namespace JetBrains.Rd
{
    public struct ClientId
    {
        public readonly string Id;

        public ClientId(string id)
        {
            Id = id;
        }

        public override string ToString()
        {
            return Id;
        }

        public const string LocalId = "Host";

        private static readonly ThreadLocal<ClientId?> ourCurrentClientId = new ThreadLocal<ClientId?>(() => null);
#if !NET35
        private static readonly AsyncLocal<ClientId?> ourAsyncLocalClientId = new AsyncLocal<ClientId?>();
#endif
        
        
        public static readonly CtxReadDelegate<ClientId> ReadDelegate = (ctx, reader) => new ClientId(reader.ReadString());
        public static readonly CtxWriteDelegate<ClientId> WriteDelegate = (ctx, writer, value) => writer.Write(value.Id);


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
                ourCurrentClientId.Value = newClientId;
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
            ourAsyncLocalClientId.Value ??
#endif
            ourCurrentClientId.Value;
    }
}
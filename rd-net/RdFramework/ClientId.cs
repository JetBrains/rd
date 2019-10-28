using System;
using JetBrains.Annotations;
using JetBrains.Rd.Impl;

namespace JetBrains.Rd
{
    /// <summary>
    /// ClientId is a global context class that is used to distinguish the originator of an action in multi-client systems
    /// In such systems, each client has their own ClientId.
    /// 
    /// The context is automatically propagated across async/await calls using AsyncLocal. The application should take care to preserve and propagate the current value across other kinds of asynchronous calls. 
    /// </summary>
    public struct ClientId : IEquatable<ClientId>
    {
        [NotNull] public readonly string Value;
        
        public ClientId([NotNull] string value)
        {
            Value = value;
        }

        public enum AbsenceBehavior
        {
            RETURN_LOCAL,
            THROW
        }

        public static AbsenceBehavior AbsenceBehaviorValue = AbsenceBehavior.RETURN_LOCAL;

        public override string ToString()
        {
            return $"ClientId({Value})";
        }

        public static readonly ClientId LocalId = new ClientId("Host");
        
        public static readonly RdContext<string> Context = new RdContext<string>("ClientId", true, Serializers.ReadString, Serializers.WriteString);

        public static readonly CtxReadDelegate<ClientId> ReadDelegate = (ctx, reader) => new ClientId(reader.ReadString());
        public static readonly CtxWriteDelegate<ClientId> WriteDelegate = (ctx, writer, value) => writer.Write(value.Value);


        #region Cookie

        public struct ClientIdCookie : IDisposable
        {
            private readonly ClientId? myOldClientId;

            public ClientIdCookie(ClientId? newClientId)
            {
                myOldClientId = CurrentOrNull;
                SetClientId(newClientId);
            }

            private static void SetClientId(ClientId? newClientId)
            {
              Context.Value = newClientId?.Value;
            }

            public void Dispose()
            {
                SetClientId(myOldClientId);
            }
        }

        #endregion

        public static ClientIdCookie CreateCookie(ClientId? clientId) => new ClientIdCookie(clientId);

        public static ClientId Current
        {
            get
            {
                switch (AbsenceBehaviorValue)
                {
                    case AbsenceBehavior.RETURN_LOCAL:
                        return CurrentOrNull ?? LocalId;
                    case AbsenceBehavior.THROW:
                        return CurrentOrNull ?? throw new NullReferenceException("ClientId not set");
                    default:
                        throw new ArgumentOutOfRangeException(nameof(AbsenceBehaviorValue));
                }
            }
        }

        [CanBeNull]
        public static ClientId? CurrentOrNull
        {
          get
          {
            var contextValue = Context.Value;
            if (contextValue == null)
              return null;
            return new ClientId(contextValue);
          }
        }

        #region Equality members

        public bool Equals(ClientId other)
        {
            return Value == other.Value;
        }

        public override bool Equals(object obj)
        {
            return obj is ClientId other && Equals(other);
        }

        public override int GetHashCode()
        {
            return Value.GetHashCode();
        }

        public static bool operator ==(ClientId left, ClientId right)
        {
            return left.Equals(right);
        }

        public static bool operator !=(ClientId left, ClientId right)
        {
            return !left.Equals(right);
        }

        #endregion
    }
}
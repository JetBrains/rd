#include "InterningRoot1.h"

#include "InterningTestModel.h"
#include "InterningNestedTestModel.h"
#include "InterningNestedTestStringModel.h"
#include "InterningProtocolLevelModel.h"
#include "InterningMtModel.h"
#include "InterningExtensionHolder.h"
#include "WrappedStringModel.h"
#include "ProtocolWrappedStringModel.h"

#include "InterningRoot1.h"
#include "InterningRoot1.h"
#include "InterningExt.h"
namespace rd {
    namespace test {
        namespace util {
            
            //companion
            
            InterningRoot1::InterningRoot1SerializersOwner const InterningRoot1::serializersOwner;
            
            void InterningRoot1::InterningRoot1SerializersOwner::registerSerializersCore(rd::Serializers const& serializers) const
            {
                serializers.registry<InterningTestModel>();
                serializers.registry<InterningNestedTestModel>();
                serializers.registry<InterningNestedTestStringModel>();
                serializers.registry<InterningProtocolLevelModel>();
                serializers.registry<InterningMtModel>();
                serializers.registry<InterningExtensionHolder>();
                serializers.registry<WrappedStringModel>();
                serializers.registry<ProtocolWrappedStringModel>();
                InterningExt::serializersOwner.registry(serializers);
            }
            
            void InterningRoot1::connect(rd::Lifetime lifetime, rd::IProtocol const * protocol)
            {
                InterningRoot1::serializersOwner.registry(protocol->get_serializers());
                
                identify(*(protocol->get_identity()), rd::RdId::Null().mix("InterningRoot1"));
                bind(lifetime, protocol, "InterningRoot1");
            }
            
            
            //initializer
            void InterningRoot1::initialize()
            {
                serializationHash = 8102192958989053575L;
            }
            
            //primary ctor
            
            //secondary constructor
            
            //default ctors and dtors
            InterningRoot1::InterningRoot1()
            {
                initialize();
            }
            
            //reader
            
            //writer
            
            //virtual init
            void InterningRoot1::init(rd::Lifetime lifetime) const
            {
                rd::RdExtBase::init(lifetime);
            }
            
            //identify
            void InterningRoot1::identify(const rd::Identities &identities, rd::RdId const &id) const
            {
                rd::RdBindableBase::identify(identities, id);
            }
            
            //getters
            
            //intern
            
            //equals trait
            
            //equality operators
            bool operator==(const InterningRoot1 &lhs, const InterningRoot1 &rhs) {
                return &lhs == &rhs;
            };
            bool operator!=(const InterningRoot1 &lhs, const InterningRoot1 &rhs){
                return !(lhs == rhs);
            }
            
            //hash code trait
            
            //type name trait
            
            //static type name trait
        };
    };
};

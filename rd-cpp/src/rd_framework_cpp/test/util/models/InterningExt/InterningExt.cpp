#include "InterningExt.h"

#include "InterningExtRootModel.h"

#include "InterningRoot1.h"
namespace rd {
    namespace test {
        namespace util {
            
            //companion
            
            InterningExt::InterningExtSerializersOwner const InterningExt::serializersOwner;
            
            void InterningExt::InterningExtSerializersOwner::registerSerializersCore(rd::Serializers const& serializers) const
            {
                serializers.registry<InterningExtRootModel>();
            }
            
            
            
            //extension
            InterningExt const & InterningExt::getOrCreateExtensionOf(InterningExtensionHolder & pointcut)
            {
                return pointcut.getOrCreateExtension<InterningExt>("interningExt");
                
            }
            
            //initializer
            void InterningExt::initialize()
            {
                serializationHash = -2181600832385335602L;
            }
            
            //primary ctor
            InterningExt::InterningExt(rd::RdProperty<InterningExtRootModel, rd::Polymorphic<InterningExtRootModel>> root_) :
            rd::RdExtBase()
            ,root_(std::move(root_))
            {
                initialize();
            }
            
            //secondary constructor
            
            //default ctors and dtors
            InterningExt::InterningExt()
            {
                initialize();
            }
            
            //reader
            
            //writer
            
            //virtual init
            void InterningExt::init(rd::Lifetime lifetime) const
            {
                rd::RdExtBase::init(lifetime);
                bindPolymorphic(root_, lifetime, this, "root");
            }
            
            //identify
            void InterningExt::identify(const rd::Identities &identities, rd::RdId const &id) const
            {
                rd::RdBindableBase::identify(identities, id);
                identifyPolymorphic(root_, identities, id.mix(".root"));
            }
            
            //getters
            rd::RdProperty<InterningExtRootModel, rd::Polymorphic<InterningExtRootModel>> const & InterningExt::get_root() const
            {
                return root_;
            }
            
            //intern
            
            //equals trait
            
            //equality operators
            bool operator==(const InterningExt &lhs, const InterningExt &rhs) {
                return &lhs == &rhs;
            };
            bool operator!=(const InterningExt &lhs, const InterningExt &rhs){
                return !(lhs == rhs);
            }
            
            //hash code trait
            
            //type name trait
            
            //static type name trait
        };
    };
};

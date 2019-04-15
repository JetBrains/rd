#include "InterningExtensionHolder.h"


namespace rd {
    namespace test {
        namespace util {
            
            //companion
            
            //initializer
            void InterningExtensionHolder::initialize()
            {
            }
            
            //primary ctor
            
            //secondary constructor
            
            //default ctors and dtors
            InterningExtensionHolder::InterningExtensionHolder()
            {
                initialize();
            }
            
            //reader
            InterningExtensionHolder InterningExtensionHolder::read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer)
            {
                auto _id = rd::RdId::read(buffer);
                InterningExtensionHolder res{};
                withId(res, _id);
                res.mySerializationContext = ctx.withInternRootsHere(res, {"InternScopeOutOfExt"});
                return res;
            }
            
            //writer
            void InterningExtensionHolder::write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const
            {
                this->rdid.write(buffer);
                this->mySerializationContext = ctx.withInternRootsHere(*this, {"InternScopeOutOfExt"});
            }
            
            //virtual init
            void InterningExtensionHolder::init(rd::Lifetime lifetime) const
            {
                rd::RdBindableBase::init(lifetime);
            }
            
            //identify
            void InterningExtensionHolder::identify(const rd::Identities &identities, rd::RdId const &id) const
            {
                rd::RdBindableBase::identify(identities, id);
            }
            
            //getters
            
            //intern
            const rd::SerializationCtx & InterningExtensionHolder::get_serialization_context() const
            {
                if (mySerializationContext) {
                   return *mySerializationContext;
                } else {
                   throw std::invalid_argument("Attempting to get serialization context too soon for");
                }
            }
            
            //equals trait
            bool InterningExtensionHolder::equals(rd::ISerializable const& object) const
            {
                auto const &other = dynamic_cast<InterningExtensionHolder const&>(object);
                return this == &other;
            }
            
            //equality operators
            bool operator==(const InterningExtensionHolder &lhs, const InterningExtensionHolder &rhs) {
                return &lhs == &rhs;
            };
            bool operator!=(const InterningExtensionHolder &lhs, const InterningExtensionHolder &rhs){
                return !(lhs == rhs);
            }
            
            //hash code trait
            
            //type name trait
            std::string InterningExtensionHolder::type_name() const
            {
                return "InterningExtensionHolder";
            }
            
            //static type name trait
            std::string InterningExtensionHolder::static_type_name()
            {
                return "InterningExtensionHolder";
            }
        };
    };
};

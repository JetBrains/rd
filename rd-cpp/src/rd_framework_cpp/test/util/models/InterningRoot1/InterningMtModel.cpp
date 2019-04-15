#include "InterningMtModel.h"


namespace rd {
    namespace test {
        namespace util {
            
            //companion
            
            //initializer
            void InterningMtModel::initialize()
            {
                signaller_.async = true;
                signaller2_.async = true;
            }
            
            //primary ctor
            InterningMtModel::InterningMtModel(rd::Wrapper<std::wstring> searchLabel_, rd::RdSignal<std::wstring, InterningMtModel::__StringInternedAtTestInternScopeSerializer> signaller_, rd::RdSignal<std::wstring, InterningMtModel::__StringInternedAtTestInternScopeSerializer> signaller2_) :
            rd::IPolymorphicSerializable(), rd::RdBindableBase()
            ,searchLabel_(std::move(searchLabel_)), signaller_(std::move(signaller_)), signaller2_(std::move(signaller2_))
            {
                initialize();
            }
            
            //secondary constructor
            InterningMtModel::InterningMtModel(rd::Wrapper<std::wstring> searchLabel_) : 
            InterningMtModel((std::move(searchLabel_)),{},{})
            {
                initialize();
            }
            
            //default ctors and dtors
            
            //reader
            InterningMtModel InterningMtModel::read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer)
            {
                auto _id = rd::RdId::read(buffer);
                auto searchLabel_ = buffer.readWString();
                auto signaller_ = rd::RdSignal<std::wstring, InterningMtModel::__StringInternedAtTestInternScopeSerializer>::read(ctx, buffer);
                auto signaller2_ = rd::RdSignal<std::wstring, InterningMtModel::__StringInternedAtTestInternScopeSerializer>::read(ctx, buffer);
                InterningMtModel res{std::move(searchLabel_), std::move(signaller_), std::move(signaller2_)};
                withId(res, _id);
                res.mySerializationContext = ctx.withInternRootsHere(res, {"TestInternScope"});
                return res;
            }
            
            //writer
            void InterningMtModel::write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const
            {
                this->rdid.write(buffer);
                buffer.writeWString(searchLabel_);
                signaller_.write(ctx, buffer);
                signaller2_.write(ctx, buffer);
                this->mySerializationContext = ctx.withInternRootsHere(*this, {"TestInternScope"});
            }
            
            //virtual init
            void InterningMtModel::init(rd::Lifetime lifetime) const
            {
                rd::RdBindableBase::init(lifetime);
                bindPolymorphic(signaller_, lifetime, this, "signaller");
                bindPolymorphic(signaller2_, lifetime, this, "signaller2");
            }
            
            //identify
            void InterningMtModel::identify(const rd::Identities &identities, rd::RdId const &id) const
            {
                rd::RdBindableBase::identify(identities, id);
                identifyPolymorphic(signaller_, identities, id.mix(".signaller"));
                identifyPolymorphic(signaller2_, identities, id.mix(".signaller2"));
            }
            
            //getters
            std::wstring const & InterningMtModel::get_searchLabel() const
            {
                return *searchLabel_;
            }
            rd::RdSignal<std::wstring, InterningMtModel::__StringInternedAtTestInternScopeSerializer> const & InterningMtModel::get_signaller() const
            {
                return signaller_;
            }
            rd::RdSignal<std::wstring, InterningMtModel::__StringInternedAtTestInternScopeSerializer> const & InterningMtModel::get_signaller2() const
            {
                return signaller2_;
            }
            
            //intern
            const rd::SerializationCtx & InterningMtModel::get_serialization_context() const
            {
                if (mySerializationContext) {
                   return *mySerializationContext;
                } else {
                   throw std::invalid_argument("Attempting to get serialization context too soon for");
                }
            }
            
            //equals trait
            bool InterningMtModel::equals(rd::ISerializable const& object) const
            {
                auto const &other = dynamic_cast<InterningMtModel const&>(object);
                return this == &other;
            }
            
            //equality operators
            bool operator==(const InterningMtModel &lhs, const InterningMtModel &rhs) {
                return &lhs == &rhs;
            };
            bool operator!=(const InterningMtModel &lhs, const InterningMtModel &rhs){
                return !(lhs == rhs);
            }
            
            //hash code trait
            
            //type name trait
            std::string InterningMtModel::type_name() const
            {
                return "InterningMtModel";
            }
            
            //static type name trait
            std::string InterningMtModel::static_type_name()
            {
                return "InterningMtModel";
            }
        };
    };
};

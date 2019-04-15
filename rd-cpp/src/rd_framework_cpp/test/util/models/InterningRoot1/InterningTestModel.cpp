#include "InterningTestModel.h"


namespace rd {
    namespace test {
        namespace util {
            
            //companion
            
            //initializer
            void InterningTestModel::initialize()
            {
                issues_.optimize_nested = true;
            }
            
            //primary ctor
            InterningTestModel::InterningTestModel(rd::Wrapper<std::wstring> searchLabel_, rd::RdMap<int32_t, WrappedStringModel, rd::Polymorphic<int32_t>, rd::Polymorphic<WrappedStringModel>> issues_) :
            rd::IPolymorphicSerializable(), rd::RdBindableBase()
            ,searchLabel_(std::move(searchLabel_)), issues_(std::move(issues_))
            {
                initialize();
            }
            
            //secondary constructor
            InterningTestModel::InterningTestModel(rd::Wrapper<std::wstring> searchLabel_) : 
            InterningTestModel((std::move(searchLabel_)),{})
            {
                initialize();
            }
            
            //default ctors and dtors
            
            //reader
            InterningTestModel InterningTestModel::read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer)
            {
                auto _id = rd::RdId::read(buffer);
                auto searchLabel_ = buffer.readWString();
                auto issues_ = rd::RdMap<int32_t, WrappedStringModel, rd::Polymorphic<int32_t>, rd::Polymorphic<WrappedStringModel>>::read(ctx, buffer);
                InterningTestModel res{std::move(searchLabel_), std::move(issues_)};
                withId(res, _id);
                res.mySerializationContext = ctx.withInternRootsHere(res, {"TestInternScope"});
                return res;
            }
            
            //writer
            void InterningTestModel::write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const
            {
                this->rdid.write(buffer);
                buffer.writeWString(searchLabel_);
                issues_.write(ctx, buffer);
                this->mySerializationContext = ctx.withInternRootsHere(*this, {"TestInternScope"});
            }
            
            //virtual init
            void InterningTestModel::init(rd::Lifetime lifetime) const
            {
                rd::RdBindableBase::init(lifetime);
                bindPolymorphic(issues_, lifetime, this, "issues");
            }
            
            //identify
            void InterningTestModel::identify(const rd::Identities &identities, rd::RdId const &id) const
            {
                rd::RdBindableBase::identify(identities, id);
                identifyPolymorphic(issues_, identities, id.mix(".issues"));
            }
            
            //getters
            std::wstring const & InterningTestModel::get_searchLabel() const
            {
                return *searchLabel_;
            }
            rd::RdMap<int32_t, WrappedStringModel, rd::Polymorphic<int32_t>, rd::Polymorphic<WrappedStringModel>> const & InterningTestModel::get_issues() const
            {
                return issues_;
            }
            
            //intern
            const rd::SerializationCtx & InterningTestModel::get_serialization_context() const
            {
                if (mySerializationContext) {
                   return *mySerializationContext;
                } else {
                   throw std::invalid_argument("Attempting to get serialization context too soon for");
                }
            }
            
            //equals trait
            bool InterningTestModel::equals(rd::ISerializable const& object) const
            {
                auto const &other = dynamic_cast<InterningTestModel const&>(object);
                return this == &other;
            }
            
            //equality operators
            bool operator==(const InterningTestModel &lhs, const InterningTestModel &rhs) {
                return &lhs == &rhs;
            };
            bool operator!=(const InterningTestModel &lhs, const InterningTestModel &rhs){
                return !(lhs == rhs);
            }
            
            //hash code trait
            
            //type name trait
            std::string InterningTestModel::type_name() const
            {
                return "InterningTestModel";
            }
            
            //static type name trait
            std::string InterningTestModel::static_type_name()
            {
                return "InterningTestModel";
            }
        };
    };
};

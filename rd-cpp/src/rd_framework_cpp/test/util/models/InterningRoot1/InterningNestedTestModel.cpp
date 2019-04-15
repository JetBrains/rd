#include "InterningNestedTestModel.h"


namespace rd {
    namespace test {
        namespace util {
            
            //companion
            
            //initializer
            void InterningNestedTestModel::initialize()
            {
            }
            
            //primary ctor
            InterningNestedTestModel::InterningNestedTestModel(rd::Wrapper<std::wstring> value_, rd::Wrapper<InterningNestedTestModel> inner_) :
            rd::IPolymorphicSerializable()
            ,value_(std::move(value_)), inner_(std::move(inner_))
            {
                initialize();
            }
            
            //secondary constructor
            
            //default ctors and dtors
            
            //reader
            InterningNestedTestModel InterningNestedTestModel::read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer)
            {
                auto value_ = buffer.readWString();
                auto inner_ = buffer.readNullable<InterningNestedTestModel>(
                [&ctx, &buffer]() 
                { return ctx.readInterned<InterningNestedTestModel, rd::util::getPlatformIndependentHash("TestInternScope")>(buffer, 
                [&ctx, &buffer](rd::SerializationCtx const &, rd::Buffer const &) 
                { return InterningNestedTestModel::read(ctx, buffer); }
                ); }
                );
                InterningNestedTestModel res{std::move(value_), std::move(inner_)};
                return res;
            }
            
            //writer
            void InterningNestedTestModel::write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const
            {
                buffer.writeWString(value_);
                buffer.writeNullable<InterningNestedTestModel>(inner_, 
                [&ctx, &buffer](rd::Wrapper<InterningNestedTestModel> const & it) -> void 
                { ctx.writeInterned<InterningNestedTestModel, rd::util::getPlatformIndependentHash("TestInternScope")>(buffer, it, 
                [&ctx, &buffer](rd::SerializationCtx const &, rd::Buffer const &, rd::Wrapper<InterningNestedTestModel> const & internedValue) -> void 
                { rd::Polymorphic<std::decay_t<decltype(internedValue)>>::write(ctx, buffer, internedValue); }
                ); }
                );
            }
            
            //virtual init
            
            //identify
            
            //getters
            std::wstring const & InterningNestedTestModel::get_value() const
            {
                return *value_;
            }
            rd::Wrapper<InterningNestedTestModel> const & InterningNestedTestModel::get_inner() const
            {
                return inner_;
            }
            
            //intern
            
            //equals trait
            bool InterningNestedTestModel::equals(rd::ISerializable const& object) const
            {
                auto const &other = dynamic_cast<InterningNestedTestModel const&>(object);
                if (this == &other) return true;
                if (this->value_ != other.value_) return false;
                if (this->inner_ != other.inner_) return false;
                
                return true;
            }
            
            //equality operators
            bool operator==(const InterningNestedTestModel &lhs, const InterningNestedTestModel &rhs) {
                if (lhs.type_name() != rhs.type_name()) return false;
                return lhs.equals(rhs);
            };
            bool operator!=(const InterningNestedTestModel &lhs, const InterningNestedTestModel &rhs){
                return !(lhs == rhs);
            }
            
            //hash code trait
            size_t InterningNestedTestModel::hashCode() const
            {
                size_t __r = 0;
                __r = __r * 31 + (std::hash<std::wstring>()(get_value()));
                __r = __r * 31 + (((bool)get_inner()) ? std::hash<InterningNestedTestModel>()(*get_inner()) : 0);
                return __r;
            }
            
            //type name trait
            std::string InterningNestedTestModel::type_name() const
            {
                return "InterningNestedTestModel";
            }
            
            //static type name trait
            std::string InterningNestedTestModel::static_type_name()
            {
                return "InterningNestedTestModel";
            }
        };
    };
};

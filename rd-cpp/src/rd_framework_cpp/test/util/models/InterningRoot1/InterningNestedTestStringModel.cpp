#include "InterningNestedTestStringModel.h"


namespace rd {
    namespace test {
        namespace util {
            
            //companion
            
            //initializer
            void InterningNestedTestStringModel::initialize()
            {
            }
            
            //primary ctor
            InterningNestedTestStringModel::InterningNestedTestStringModel(rd::Wrapper<std::wstring> value_, rd::Wrapper<InterningNestedTestStringModel> inner_) :
            rd::IPolymorphicSerializable()
            ,value_(std::move(value_)), inner_(std::move(inner_))
            {
                initialize();
            }
            
            //secondary constructor
            
            //default ctors and dtors
            
            //reader
            InterningNestedTestStringModel InterningNestedTestStringModel::read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer)
            {
                auto value_ = ctx.readInterned<std::wstring, rd::util::getPlatformIndependentHash("TestInternScope")>(buffer, 
                [&ctx, &buffer](rd::SerializationCtx const &, rd::Buffer const &) 
                { return buffer.readWString(); }
                );
                auto inner_ = buffer.readNullable<InterningNestedTestStringModel>(
                [&ctx, &buffer]() 
                { return InterningNestedTestStringModel::read(ctx, buffer); }
                );
                InterningNestedTestStringModel res{std::move(value_), std::move(inner_)};
                return res;
            }
            
            //writer
            void InterningNestedTestStringModel::write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const
            {
                ctx.writeInterned<std::wstring, rd::util::getPlatformIndependentHash("TestInternScope")>(buffer, value_, 
                [&ctx, &buffer](rd::SerializationCtx const &, rd::Buffer const &, rd::Wrapper<std::wstring> const & internedValue) -> void 
                { buffer.writeWString(internedValue); }
                );
                buffer.writeNullable<InterningNestedTestStringModel>(inner_, 
                [&ctx, &buffer](rd::Wrapper<InterningNestedTestStringModel> const & it) -> void 
                { rd::Polymorphic<std::decay_t<decltype(it)>>::write(ctx, buffer, it); }
                );
            }
            
            //virtual init
            
            //identify
            
            //getters
            std::wstring const & InterningNestedTestStringModel::get_value() const
            {
                return *value_;
            }
            rd::Wrapper<InterningNestedTestStringModel> const & InterningNestedTestStringModel::get_inner() const
            {
                return inner_;
            }
            
            //intern
            
            //equals trait
            bool InterningNestedTestStringModel::equals(rd::ISerializable const& object) const
            {
                auto const &other = dynamic_cast<InterningNestedTestStringModel const&>(object);
                if (this == &other) return true;
                if (this->value_ != other.value_) return false;
                if (this->inner_ != other.inner_) return false;
                
                return true;
            }
            
            //equality operators
            bool operator==(const InterningNestedTestStringModel &lhs, const InterningNestedTestStringModel &rhs) {
                if (lhs.type_name() != rhs.type_name()) return false;
                return lhs.equals(rhs);
            };
            bool operator!=(const InterningNestedTestStringModel &lhs, const InterningNestedTestStringModel &rhs){
                return !(lhs == rhs);
            }
            
            //hash code trait
            size_t InterningNestedTestStringModel::hashCode() const
            {
                size_t __r = 0;
                __r = __r * 31 + (std::hash<std::wstring>()(get_value()));
                __r = __r * 31 + (((bool)get_inner()) ? std::hash<InterningNestedTestStringModel>()(*get_inner()) : 0);
                return __r;
            }
            
            //type name trait
            std::string InterningNestedTestStringModel::type_name() const
            {
                return "InterningNestedTestStringModel";
            }
            
            //static type name trait
            std::string InterningNestedTestStringModel::static_type_name()
            {
                return "InterningNestedTestStringModel";
            }
        };
    };
};

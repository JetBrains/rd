#include "WrappedStringModel.h"


namespace rd {
    namespace test {
        namespace util {
            
            //companion
            
            //initializer
            void WrappedStringModel::initialize()
            {
            }
            
            //primary ctor
            WrappedStringModel::WrappedStringModel(rd::Wrapper<std::wstring> text_) :
            rd::IPolymorphicSerializable()
            ,text_(std::move(text_))
            {
                initialize();
            }
            
            //secondary constructor
            
            //default ctors and dtors
            
            //reader
            WrappedStringModel WrappedStringModel::read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer)
            {
                auto text_ = ctx.readInterned<std::wstring, rd::util::getPlatformIndependentHash("TestInternScope")>(buffer, 
                [&ctx, &buffer](rd::SerializationCtx const &, rd::Buffer const &) 
                { return buffer.readWString(); }
                );
                WrappedStringModel res{std::move(text_)};
                return res;
            }
            
            //writer
            void WrappedStringModel::write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const
            {
                ctx.writeInterned<std::wstring, rd::util::getPlatformIndependentHash("TestInternScope")>(buffer, text_, 
                [&ctx, &buffer](rd::SerializationCtx const &, rd::Buffer const &, rd::Wrapper<std::wstring> const & internedValue) -> void 
                { buffer.writeWString(internedValue); }
                );
            }
            
            //virtual init
            
            //identify
            
            //getters
            std::wstring const & WrappedStringModel::get_text() const
            {
                return *text_;
            }
            
            //intern
            
            //equals trait
            bool WrappedStringModel::equals(rd::ISerializable const& object) const
            {
                auto const &other = dynamic_cast<WrappedStringModel const&>(object);
                if (this == &other) return true;
                if (this->text_ != other.text_) return false;
                
                return true;
            }
            
            //equality operators
            bool operator==(const WrappedStringModel &lhs, const WrappedStringModel &rhs) {
                if (lhs.type_name() != rhs.type_name()) return false;
                return lhs.equals(rhs);
            };
            bool operator!=(const WrappedStringModel &lhs, const WrappedStringModel &rhs){
                return !(lhs == rhs);
            }
            
            //hash code trait
            size_t WrappedStringModel::hashCode() const
            {
                size_t __r = 0;
                __r = __r * 31 + (std::hash<std::wstring>()(get_text()));
                return __r;
            }
            
            //type name trait
            std::string WrappedStringModel::type_name() const
            {
                return "WrappedStringModel";
            }
            
            //static type name trait
            std::string WrappedStringModel::static_type_name()
            {
                return "WrappedStringModel";
            }
        };
    };
};

#include "ProtocolWrappedStringModel.h"


namespace rd {
    namespace test {
        namespace util {
            
            //companion
            
            //initializer
            void ProtocolWrappedStringModel::initialize()
            {
            }
            
            //primary ctor
            ProtocolWrappedStringModel::ProtocolWrappedStringModel(rd::Wrapper<std::wstring> text_) :
            rd::IPolymorphicSerializable()
            ,text_(std::move(text_))
            {
                initialize();
            }
            
            //secondary constructor
            
            //default ctors and dtors
            
            //reader
            ProtocolWrappedStringModel ProtocolWrappedStringModel::read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer)
            {
                auto text_ = ctx.readInterned<std::wstring, rd::util::getPlatformIndependentHash("Protocol")>(buffer, 
                [&ctx, &buffer](rd::SerializationCtx const &, rd::Buffer const &) 
                { return buffer.readWString(); }
                );
                ProtocolWrappedStringModel res{std::move(text_)};
                return res;
            }
            
            //writer
            void ProtocolWrappedStringModel::write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const
            {
                ctx.writeInterned<std::wstring, rd::util::getPlatformIndependentHash("Protocol")>(buffer, text_, 
                [&ctx, &buffer](rd::SerializationCtx const &, rd::Buffer const &, rd::Wrapper<std::wstring> const & internedValue) -> void 
                { buffer.writeWString(internedValue); }
                );
            }
            
            //virtual init
            
            //identify
            
            //getters
            std::wstring const & ProtocolWrappedStringModel::get_text() const
            {
                return *text_;
            }
            
            //intern
            
            //equals trait
            bool ProtocolWrappedStringModel::equals(rd::ISerializable const& object) const
            {
                auto const &other = dynamic_cast<ProtocolWrappedStringModel const&>(object);
                if (this == &other) return true;
                if (this->text_ != other.text_) return false;
                
                return true;
            }
            
            //equality operators
            bool operator==(const ProtocolWrappedStringModel &lhs, const ProtocolWrappedStringModel &rhs) {
                if (lhs.type_name() != rhs.type_name()) return false;
                return lhs.equals(rhs);
            };
            bool operator!=(const ProtocolWrappedStringModel &lhs, const ProtocolWrappedStringModel &rhs){
                return !(lhs == rhs);
            }
            
            //hash code trait
            size_t ProtocolWrappedStringModel::hashCode() const
            {
                size_t __r = 0;
                __r = __r * 31 + (std::hash<std::wstring>()(get_text()));
                return __r;
            }
            
            //type name trait
            std::string ProtocolWrappedStringModel::type_name() const
            {
                return "ProtocolWrappedStringModel";
            }
            
            //static type name trait
            std::string ProtocolWrappedStringModel::static_type_name()
            {
                return "ProtocolWrappedStringModel";
            }
        };
    };
};

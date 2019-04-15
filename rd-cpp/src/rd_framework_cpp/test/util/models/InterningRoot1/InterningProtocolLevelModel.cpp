#include "InterningProtocolLevelModel.h"


namespace rd {
    namespace test {
        namespace util {
            
            //companion
            
            //initializer
            void InterningProtocolLevelModel::initialize()
            {
                issues_.optimize_nested = true;
            }
            
            //primary ctor
            InterningProtocolLevelModel::InterningProtocolLevelModel(rd::Wrapper<std::wstring> searchLabel_, rd::RdMap<int32_t, ProtocolWrappedStringModel, rd::Polymorphic<int32_t>, rd::Polymorphic<ProtocolWrappedStringModel>> issues_) :
            rd::IPolymorphicSerializable(), rd::RdBindableBase()
            ,searchLabel_(std::move(searchLabel_)), issues_(std::move(issues_))
            {
                initialize();
            }
            
            //secondary constructor
            InterningProtocolLevelModel::InterningProtocolLevelModel(rd::Wrapper<std::wstring> searchLabel_) : 
            InterningProtocolLevelModel((std::move(searchLabel_)),{})
            {
                initialize();
            }
            
            //default ctors and dtors
            
            //reader
            InterningProtocolLevelModel InterningProtocolLevelModel::read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer)
            {
                auto _id = rd::RdId::read(buffer);
                auto searchLabel_ = buffer.readWString();
                auto issues_ = rd::RdMap<int32_t, ProtocolWrappedStringModel, rd::Polymorphic<int32_t>, rd::Polymorphic<ProtocolWrappedStringModel>>::read(ctx, buffer);
                InterningProtocolLevelModel res{std::move(searchLabel_), std::move(issues_)};
                withId(res, _id);
                return res;
            }
            
            //writer
            void InterningProtocolLevelModel::write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const
            {
                this->rdid.write(buffer);
                buffer.writeWString(searchLabel_);
                issues_.write(ctx, buffer);
            }
            
            //virtual init
            void InterningProtocolLevelModel::init(rd::Lifetime lifetime) const
            {
                rd::RdBindableBase::init(lifetime);
                bindPolymorphic(issues_, lifetime, this, "issues");
            }
            
            //identify
            void InterningProtocolLevelModel::identify(const rd::Identities &identities, rd::RdId const &id) const
            {
                rd::RdBindableBase::identify(identities, id);
                identifyPolymorphic(issues_, identities, id.mix(".issues"));
            }
            
            //getters
            std::wstring const & InterningProtocolLevelModel::get_searchLabel() const
            {
                return *searchLabel_;
            }
            rd::RdMap<int32_t, ProtocolWrappedStringModel, rd::Polymorphic<int32_t>, rd::Polymorphic<ProtocolWrappedStringModel>> const & InterningProtocolLevelModel::get_issues() const
            {
                return issues_;
            }
            
            //intern
            
            //equals trait
            bool InterningProtocolLevelModel::equals(rd::ISerializable const& object) const
            {
                auto const &other = dynamic_cast<InterningProtocolLevelModel const&>(object);
                return this == &other;
            }
            
            //equality operators
            bool operator==(const InterningProtocolLevelModel &lhs, const InterningProtocolLevelModel &rhs) {
                return &lhs == &rhs;
            };
            bool operator!=(const InterningProtocolLevelModel &lhs, const InterningProtocolLevelModel &rhs){
                return !(lhs == rhs);
            }
            
            //hash code trait
            
            //type name trait
            std::string InterningProtocolLevelModel::type_name() const
            {
                return "InterningProtocolLevelModel";
            }
            
            //static type name trait
            std::string InterningProtocolLevelModel::static_type_name()
            {
                return "InterningProtocolLevelModel";
            }
        };
    };
};

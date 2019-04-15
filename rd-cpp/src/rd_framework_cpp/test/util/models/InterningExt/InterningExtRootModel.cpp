#include "InterningExtRootModel.h"


namespace rd {
    namespace test {
        namespace util {
            
            //companion
            
            //initializer
            void InterningExtRootModel::initialize()
            {
                internedLocally_.optimize_nested = true;
                internedExternally_.optimize_nested = true;
                internedInProtocol_.optimize_nested = true;
            }
            
            //primary ctor
            InterningExtRootModel::InterningExtRootModel(rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtInternScopeInExtSerializer> internedLocally_, rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtInternScopeOutOfExtSerializer> internedExternally_, rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtProtocolSerializer> internedInProtocol_) :
            rd::IPolymorphicSerializable(), rd::RdBindableBase()
            ,internedLocally_(std::move(internedLocally_)), internedExternally_(std::move(internedExternally_)), internedInProtocol_(std::move(internedInProtocol_))
            {
                initialize();
            }
            
            //secondary constructor
            
            //default ctors and dtors
            InterningExtRootModel::InterningExtRootModel()
            {
                initialize();
            }
            
            //reader
            InterningExtRootModel InterningExtRootModel::read(rd::SerializationCtx const& ctx, rd::Buffer const & buffer)
            {
                auto _id = rd::RdId::read(buffer);
                auto internedLocally_ = rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtInternScopeInExtSerializer>::read(ctx, buffer);
                auto internedExternally_ = rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtInternScopeOutOfExtSerializer>::read(ctx, buffer);
                auto internedInProtocol_ = rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtProtocolSerializer>::read(ctx, buffer);
                InterningExtRootModel res{std::move(internedLocally_), std::move(internedExternally_), std::move(internedInProtocol_)};
                withId(res, _id);
                res.mySerializationContext = ctx.withInternRootsHere(res, {"InternScopeInExt"});
                return res;
            }
            
            //writer
            void InterningExtRootModel::write(rd::SerializationCtx const& ctx, rd::Buffer const& buffer) const
            {
                this->rdid.write(buffer);
                internedLocally_.write(ctx, buffer);
                internedExternally_.write(ctx, buffer);
                internedInProtocol_.write(ctx, buffer);
                this->mySerializationContext = ctx.withInternRootsHere(*this, {"InternScopeInExt"});
            }
            
            //virtual init
            void InterningExtRootModel::init(rd::Lifetime lifetime) const
            {
                rd::RdBindableBase::init(lifetime);
                bindPolymorphic(internedLocally_, lifetime, this, "internedLocally");
                bindPolymorphic(internedExternally_, lifetime, this, "internedExternally");
                bindPolymorphic(internedInProtocol_, lifetime, this, "internedInProtocol");
            }
            
            //identify
            void InterningExtRootModel::identify(const rd::Identities &identities, rd::RdId const &id) const
            {
                rd::RdBindableBase::identify(identities, id);
                identifyPolymorphic(internedLocally_, identities, id.mix(".internedLocally"));
                identifyPolymorphic(internedExternally_, identities, id.mix(".internedExternally"));
                identifyPolymorphic(internedInProtocol_, identities, id.mix(".internedInProtocol"));
            }
            
            //getters
            rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtInternScopeInExtSerializer> const & InterningExtRootModel::get_internedLocally() const
            {
                return internedLocally_;
            }
            rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtInternScopeOutOfExtSerializer> const & InterningExtRootModel::get_internedExternally() const
            {
                return internedExternally_;
            }
            rd::RdProperty<std::wstring, InterningExtRootModel::__StringInternedAtProtocolSerializer> const & InterningExtRootModel::get_internedInProtocol() const
            {
                return internedInProtocol_;
            }
            
            //intern
            const rd::SerializationCtx & InterningExtRootModel::get_serialization_context() const
            {
                if (mySerializationContext) {
                   return *mySerializationContext;
                } else {
                   throw std::invalid_argument("Attempting to get serialization context too soon for");
                }
            }
            
            //equals trait
            bool InterningExtRootModel::equals(rd::ISerializable const& object) const
            {
                auto const &other = dynamic_cast<InterningExtRootModel const&>(object);
                return this == &other;
            }
            
            //equality operators
            bool operator==(const InterningExtRootModel &lhs, const InterningExtRootModel &rhs) {
                return &lhs == &rhs;
            };
            bool operator!=(const InterningExtRootModel &lhs, const InterningExtRootModel &rhs){
                return !(lhs == rhs);
            }
            
            //hash code trait
            
            //type name trait
            std::string InterningExtRootModel::type_name() const
            {
                return "InterningExtRootModel";
            }
            
            //static type name trait
            std::string InterningExtRootModel::static_type_name()
            {
                return "InterningExtRootModel";
            }
        };
    };
};

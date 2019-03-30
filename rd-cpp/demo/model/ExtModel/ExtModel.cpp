#include "ExtModel.h"


#include "DemoRoot.h"
namespace demo {
    
    //companion
    
    ExtModel::ExtModelSerializersOwner const ExtModel::serializersOwner;
    
    void ExtModel::ExtModelSerializersOwner::registerSerializersCore(rd::Serializers const& serializers) const
    {
    }
    
    
    
    //extension
    ExtModel const & ExtModel::getOrCreateExtensionOf(DemoModel & pointcut)
    {
        return pointcut.getOrCreateExtension<ExtModel>("extModel");
        
    }
    
    //initializer
    void ExtModel::initialize()
    {
        serializationHash = 2364843396187734L;
    }
    
    //primary ctor
    ExtModel::ExtModel(rd::RdSignal<rd::Void, rd::Polymorphic<rd::Void>> checker_) :
    rd::RdExtBase()
    ,checker_(std::move(checker_))
    {
        initialize();
    }
    
    //secondary constructor
    
    //default ctors and dtors
    ExtModel::ExtModel()
    {
        initialize();
    }
    
    //reader
    
    //writer
    
    //virtual init
    void ExtModel::init(rd::Lifetime lifetime) const
    {
        rd::RdExtBase::init(lifetime);
        bindPolymorphic(checker_, lifetime, this, "checker");
    }
    
    //identify
    void ExtModel::identify(const rd::Identities &identities, rd::RdId const &id) const
    {
        rd::RdBindableBase::identify(identities, id);
        identifyPolymorphic(checker_, identities, id.mix(".checker"));
    }
    
    //getters
    rd::RdSignal<rd::Void, rd::Polymorphic<rd::Void>> const & ExtModel::get_checker() const
    {
        return checker_;
    }
    
    //intern
    
    //equals trait
    
    //equality operators
    bool operator==(const ExtModel &lhs, const ExtModel &rhs){
        return &lhs == &rhs;
    }
    bool operator!=(const ExtModel &lhs, const ExtModel &rhs){
        return !(lhs == rhs);
    }
    
    //hash code trait
};

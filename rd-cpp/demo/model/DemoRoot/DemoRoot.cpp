#include "DemoRoot.h"


#include "DemoRoot.h"
#include "DemoRoot.h"
#include "DemoModel.h"
#include "ExtModel.h"
namespace demo {
    
    //companion
    
    DemoRoot::DemoRootSerializersOwner const DemoRoot::serializersOwner;
    
    void DemoRoot::DemoRootSerializersOwner::registerSerializersCore(rd::Serializers const& serializers) const
    {
        DemoModel::serializersOwner.registry(serializers);
        ExtModel::serializersOwner.registry(serializers);
    }
    
    void DemoRoot::connect(rd::Lifetime lifetime, rd::IProtocol const * protocol)
    {
        DemoRoot::serializersOwner.registry(protocol->get_serializers());
        
        identify(*(protocol->get_identity()), rd::RdId::Null().mix("DemoRoot"));
        bind(lifetime, protocol, "DemoRoot");
    }
    
    
    //initializer
    void DemoRoot::initialize()
    {
        serializationHash = 2990580803186469991L;
    }
    
    //primary ctor
    
    //secondary constructor
    
    //default ctors and dtors
    DemoRoot::DemoRoot()
    {
        initialize();
    }
    
    //reader
    
    //writer
    
    //virtual init
    void DemoRoot::init(rd::Lifetime lifetime) const
    {
        rd::RdExtBase::init(lifetime);
    }
    
    //identify
    void DemoRoot::identify(const rd::Identities &identities, rd::RdId const &id) const
    {
        rd::RdBindableBase::identify(identities, id);
    }
    
    //getters
    
    //intern
    
    //equals trait
    
    //equality operators
    bool operator==(const DemoRoot &lhs, const DemoRoot &rhs) {
        return &lhs == &rhs;
    };
    bool operator!=(const DemoRoot &lhs, const DemoRoot &rhs){
        return !(lhs == rhs);
    }
    
    //hash code trait
    
    //type name trait
    
    //static type name trait
    
    //to string trait
    std::string to_string(const demo::DemoRoot & value)
    {
        std::string res = "DemoRoot\n";
        return res;
    }
};

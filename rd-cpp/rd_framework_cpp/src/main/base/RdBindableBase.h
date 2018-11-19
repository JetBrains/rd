//
// Created by jetbrains on 23.07.2018.
//

#ifndef RD_CPP_RDBINDABLEBASE_H
#define RD_CPP_RDBINDABLEBASE_H

#include "demangle.h"
#include "IRdBindable.h"
#include "IProtocol.h"

//#include <any>

class RdBindableBase : public virtual IRdBindable/*, IPrintable*/ {
protected:
    mutable IRdDynamic const *parent = nullptr;

    mutable tl::optional<Lifetime> bind_lifetime;

    //bound state: inferred

    bool is_bound() const;

    const IProtocol *const get_protocol() const override;

    mutable std::vector<std::pair<std::string, IRdBindable *> > bindableChildren;
    //mutable std::vector<std::pair<std::string, std::any> > nonBindableChildren;

    SerializationCtx const &get_serialization_context() const override;

public:
    //region ctor/dtor

    RdBindableBase() {
        location = RName("<<not bound>>");
    };

    RdBindableBase(RdBindableBase &&) = default;

    RdBindableBase &operator=(RdBindableBase &&) = default;

    virtual ~RdBindableBase() = default;
    //endregion

    //need to implement in subclasses
    virtual void init(Lifetime lifetime) const;

    void bind(Lifetime lf, IRdDynamic const *parent, const std::string &name) const override;


//    /*concurrent*/std::map<std::string, std::any> extensions;

//    getOrCreateExtension

    void identify(const IIdentities &identities, RdId id) const override;

    mutable std::map<std::string, std::shared_ptr<IRdBindable> > bindable_extensions;//todo concurrency
    //mutable std::map<std::string, std::any> non_bindable_extensions;//todo concurrency

    template<typename T, typename F>
    typename std::enable_if<std::is_base_of<IRdBindable, T>::value, T>::type const &
    getOrCreateExtension(std::string const &name, F &&create) const {
        if (bindable_extensions.count(name) > 0) {
            return *dynamic_cast<T const *>(bindable_extensions[name].get());
        } else {
            std::shared_ptr<IRdBindable> new_extension = std::make_shared<T>(create());
            T const &res = *dynamic_cast<T const *>(new_extension.get());
            if (bind_lifetime.has_value()) {
                auto protocol = get_protocol();
                new_extension->identify(*protocol->identity, rdid.mix("." + name));
                new_extension->bind(*bind_lifetime, this, name);
            }
            bindable_extensions.emplace(name, std::move(new_extension));
            return res;
        }
    }

	/* template<typename T>
	 std::enable_if_t<!std::is_base_of_v<IRdBindable, T>, T> const &
	 getOrCreateExtension(std::string const &name, std::function<T()> create) const {
		 if (non_bindable_extensions.count(name) == 0) {
			 return std::any_cast<T const &>(non_bindable_extensions[name] = create());
		 }
		 return std::any_cast<T const &>(non_bindable_extensions.at(name));
	 }*/
};

//T : RdBindableBase
template<typename T>
T &withId(T &that, RdId const &id) {
    MY_ASSERT_MSG(that.rdid == RdId::Null(), "this.id != RdId.NULL_ID, but ${this.rdid}");
    MY_ASSERT_MSG((id != RdId::Null()), "id != RdId.NULL_ID");

    that.rdid = id;
    return that;
}

template<typename T>
T &statics(T &that, int64_t id) {
    MY_ASSERT_MSG((id > 0 && id < RdId::MAX_STATIC_ID),
                  ("Expected id > 0 && id < RdId.MaxStaticId, got " + std::to_string(id)));
    return withId(that, RdId(static_cast<int64_t >(id)));
}

template<typename T>
T &withIdFromName(T &that, std::string const &name) {
    return withId(that, RdId::Null().mix(name));
}


#endif //RD_CPP_RDBINDABLEBASE_H

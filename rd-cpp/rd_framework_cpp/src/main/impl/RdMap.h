//
// Created by jetbrains on 02.08.2018.
//

#ifndef RD_CPP_RDMAP_H
#define RD_CPP_RDMAP_H

#include "ViewableMap.h"
#include "RdReactiveBase.h"
#include "Polymorphic.h"

#include <cstdint>

template<typename K, typename V, typename KS = Polymorphic<K>, typename VS = Polymorphic<V>>
class RdMap : public RdReactiveBase, public IViewableMap<K, V>, public ISerializable {
private:
    ViewableMap<K, V> map;
    mutable int64_t nextVersion = 0;
    mutable std::unordered_map<K, int64_t> pendingForAck;

    bool is_master() const {
        return master;
    }

    std::string logmsg(Op op, int64_t version, K const *key, V const *value = nullptr) const {
        return "map " + location.toString() + " " + rdid.toString() + ":: " + to_string(op) +
               ":: key = " + to_string(*key) +
               ((version > 0) ? " :: version = " + /*std::*/to_string(version) : "") +
               " :: value = " + (value ? to_string(*value) : "");
    }

    std::string logmsg(Op op, int64_t version, K const *key, tl::optional<V> const &value) const {
        return logmsg(op, version, key, value.has_value() ? &value.value() : nullptr);
    }

public:
    bool master = false;

    bool optimize_nested = false;

    using Event = typename IViewableMap<K, V>::Event;

	using key_type = K;
	using value_type = V;

    //region ctor/dtor

    RdMap() = default;

    RdMap(RdMap &&) = default;

    RdMap &operator=(RdMap &&) = default;

    virtual ~RdMap() = default;
    //endregion

    static RdMap<K, V, KS, VS> read(SerializationCtx const &ctx, Buffer const &buffer) {
        RdMap<K, V, KS, VS> res;
        RdId id = RdId::read(buffer);
        withId(res, id);
        return res;
    }

    void write(SerializationCtx const &ctx, Buffer const &buffer) const override {
        rdid.write(buffer);
    }

    static const int32_t versionedFlagShift = 8;

    void init(Lifetime lifetime) const override {
        RdBindableBase::init(lifetime);

        local_change([this, lifetime]() {
            advise(lifetime, [this, lifetime](Event e) {
                if (!is_local_change) return;

                V const *new_value = e.get_new_value();
                if (new_value) {
                    const IProtocol *iProtocol = get_protocol();
                    identifyPolymorphic(*new_value, *iProtocol->identity, iProtocol->identity->next(rdid));
                }

                get_wire()->send(rdid, [this, e](Buffer const &buffer) {
                    int32_t versionedFlag = ((is_master() ? 1 : 0)) << versionedFlagShift;
                    Op op = static_cast<Op>(e.v.index());

                    buffer.write_pod<int32_t>(static_cast<int32_t>(op) | versionedFlag);

                    int64_t version = is_master() ? ++nextVersion : 0L;

                    if (is_master()) {
                        pendingForAck.insert(std::make_pair(*e.get_key(), version));
                        buffer.write_pod(version);
                    }

                    KS::write(this->get_serialization_context(), buffer, *e.get_key());

                    V const *new_value = e.get_new_value();
                    if (new_value) {
                        VS::write(this->get_serialization_context(), buffer, *new_value);
                    }

                    logSend.trace(logmsg(op, nextVersion - 1, e.get_key(), new_value));
                });
            });
        });

        get_wire()->advise(lifetime, this);

        if (!optimize_nested)
            this->view(lifetime, [this](Lifetime lf, std::pair<K const *, V const *> entry) {
                bindPolymorphic(entry.second, lf, this, "[" + to_string(*entry.first) + "]");
            });
    }

    void on_wire_received(Buffer buffer) const override {
        int32_t header = buffer.read_pod<int32_t>();
        bool msgVersioned = (header >> versionedFlagShift) != 0;
        Op op = static_cast<Op>(header & ((1 << versionedFlagShift) - 1));

        int64_t version = msgVersioned ? buffer.read_pod<int64_t>() : 0;

        K key = KS::read(this->get_serialization_context(), buffer);

        if (op == Op::ACK) {
            std::string errmsg;
            if (!msgVersioned) {
                errmsg = "Received " + to_string(Op::ACK) + " while msg hasn't versioned flag set";
            } else if (!is_master()) {
                errmsg = "Received " + to_string(Op::ACK) + " when not a Master";
            } else {
                if (pendingForAck.count(key) > 0) {
                    int64_t pendingVersion = pendingForAck.at(key);
                    if (pendingVersion < version) {
                        errmsg = "Pending version " + to_string(pendingVersion) + " < " + to_string(Op::ACK) +
                                 " version `" + to_string(version);
                    } else {
                        //side effect
                        if (pendingVersion == version) {
                            pendingForAck.erase(key); //else we don't need to remove, silently drop
                        }
                        // return good result
                    }
                } else {
                    errmsg = "No pending for " + to_string(Op::ACK);
                }
            }
            if (errmsg.empty()) {
                logReceived.trace(logmsg(Op::ACK, version, &key));
            } else {
                logReceived.error(logmsg(Op::ACK, version, &key) + " >> " + errmsg);
            }
        } else {
            bool isPut = (op == Op::ADD || op == Op::UPDATE);
            tl::optional<V> value;
            if (isPut) {
                value = VS::read(this->get_serialization_context(), buffer);
            }

            if (msgVersioned || !is_master() || pendingForAck.count(key) == 0) {
                logReceived.trace(logmsg(op, version, &key, value));
                if (value.has_value()) {
                    map.set(key, std::move(*value));
                } else {
                    map.remove(key);
                }
            } else {
                logReceived.trace(logmsg(op, version, &key, value) + " >> REJECTED");
            }

            if (msgVersioned) {
                get_wire()->send(rdid, [this, version, key = std::move(key)](Buffer const &innerBuffer) {
                    innerBuffer.write_pod<int32_t>((1 << versionedFlagShift) | static_cast<int32_t>(Op::ACK));
                    innerBuffer.write_pod<int64_t>(version);
                    KS::write(this->get_serialization_context(), innerBuffer, key);

                    logSend.trace(logmsg(Op::ACK, version, &key));
                });
                if (is_master()) {
                    logReceived.error("Both ends are masters: " + location.toString());
                }
            }
        }
    }

    void advise(Lifetime lifetime, std::function<void(Event)> handler) const override {
        if (is_bound()) {
            assert_threading();
        }
        map.advise(lifetime, handler);
    }

    V const *get(K const &key) const override {
        return local_change([&]() -> V const * { return map.get(key); });
    }

    V const *set(K key, V value) const override {
        return local_change([&]() mutable -> V const * {
            return map.set(std::move(key), std::move(value));
        });
    }

    tl::optional<V> remove(K const &key) const override {
        return local_change([&]() { return map.remove(key); });
    }

    void clear() const override {
        return local_change([&]() { return map.clear(); });
    }

    size_t size() const override {
        return map.size();
    }

    bool empty() const override {
        return map.empty();
    }
};

static_assert(std::is_move_constructible<RdMap<int, int> >::value, "Is move constructible RdMap<int, int>");
static_assert(std::is_move_assignable<RdMap<int, int> >::value, "Is move constructible RdMap<int, int>");
static_assert(std::is_default_constructible<RdMap<int, int> >::value, "Is default constructible RdMap<int, int>");

#endif //RD_CPP_RDMAP_H

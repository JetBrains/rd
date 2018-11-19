//
// Created by jetbrains on 02.08.2018.
//

#ifndef RD_CPP_RDLIST_H
#define RD_CPP_RDLIST_H

#include "ViewableList.h"
#include "RdReactiveBase.h"
#include "Polymorphic.h"
#include "SerializationCtx.h"
#include "demangle.h"

template<typename V, typename S = Polymorphic<V>>
class RdList : public RdReactiveBase, public IViewableList<V>, public ISerializable {
private:
    mutable ViewableList<V> list;
    mutable int64_t nextVersion = 1;

    using Event = typename IViewableList<V>::Event;
public:
    //region ctor/dtor

    RdList() = default;

    RdList(RdList &&) = default;

    RdList &operator=(RdList &&) = default;

    virtual ~RdList() = default;
    //endregion

    static RdList<V, S> read(SerializationCtx const &ctx, Buffer const &buffer) {
        RdList<V, S> result;
        return withId(result, RdId::read(buffer));
    }

    void write(SerializationCtx const &ctx, Buffer const &buffer) const override {
        buffer.write_pod<int64_t>(nextVersion);
        rdid.write(buffer);
    }

    static const int32_t versionedFlagShift = 2; // update when changing Op

    bool optimizeNested = false;

    std::string logmsg(Op op, int64_t version, int32_t key, V const *value = nullptr) const {
        return "list " + location.toString() + " " + rdid.toString() + ":: " + to_string(op) +
               ":: key = " + std::to_string(key) +
               ((version > 0) ? " :: version = " + /*std::*/to_string(version) : "") +
               " :: value = " + (value ? to_string(*value) : "");
    }

    void init(Lifetime lifetime) const override {
        RdBindableBase::init(lifetime);

        local_change([this, lifetime]() {
            advise(lifetime, [this, lifetime](typename IViewableList<V>::Event e) {
                if (!is_local_change) return;

                if (!optimizeNested) {
                    V const *new_value = e.get_new_value();
                    if (new_value) {
                        const IProtocol *iProtocol = get_protocol();
                        identifyPolymorphic(*new_value, *iProtocol->identity,
                                            iProtocol->identity->next(rdid));
                    }
                }

                get_wire()->send(rdid, [this, &e](Buffer const &buffer) {
                    Op op = static_cast<Op >(e.v.index());

                    buffer.write_pod<int64_t>(static_cast<int64_t>(op) | (nextVersion++ << versionedFlagShift));
                    buffer.write_pod<int32_t>(static_cast<const int32_t>(e.get_index()));

                    V const *new_value = e.get_new_value();
                    if (new_value) {
                        S::write(this->get_serialization_context(), buffer, *new_value);
                    }
                    this->logSend.trace(logmsg(op, nextVersion - 1, e.get_index(), new_value));
                });
            });
        });

        get_wire()->advise(lifetime, this);

        if (!optimizeNested)
            this->view(lifetime, [this](Lifetime lf, size_t index, V const &value) {
                bindPolymorphic(value, lf, this, "[" + std::to_string(index) + "]");
            });
    }

    void on_wire_received(Buffer buffer) const override {
        int64_t header = (buffer.read_pod<int64_t>());
        int64_t version = header >> versionedFlagShift;
        Op op = static_cast<Op>((header & ((1 << versionedFlagShift) - 1L)));
        int32_t index = (buffer.read_pod<int32_t>());

        MY_ASSERT_MSG(version == nextVersion, ("Version conflict for " + location.toString() + "}. Expected version " +
                                               std::to_string(nextVersion) +
                                               ", received " +
                                               std::to_string(version) +
                                               ". Are you modifying a list from two sides?"));

        nextVersion++;

        switch (op) {
            case Op::ADD: {
                V value = S::read(this->get_serialization_context(), buffer);

                this->logReceived.trace(logmsg(op, version, index, &value));

                (index < 0) ? list.add(std::move(value)) : list.add(static_cast<size_t>(index), std::move(value));
                break;
            }
            case Op::UPDATE: {
                V value = S::read(this->get_serialization_context(), buffer);

                this->logReceived.trace(logmsg(op, version, index, &value));

                list.set(static_cast<size_t>(index), std::move(value));
                break;
            }
            case Op::REMOVE: {
                this->logReceived.trace(logmsg(op, version, index));

                list.removeAt(static_cast<size_t>(index));
                break;
            }
            case Op::ACK:
                break;
        }
    }

    void advise(Lifetime lifetime, std::function<void(Event)> handler) const override {
        if (is_bound()) assert_threading();
        list.advise(std::move(lifetime), handler);
    }

    bool add(V element) const override { return local_change<bool>([&]() { return list.add(std::move(element)); }); }

    bool add(size_t index, V element) const override {
        return local_change<bool>([&]() { return list.add(index, std::move(element)); });
    }

    bool remove(V const &element) const override { return local_change<bool>([&]() { return list.remove(element); }); }

    V removeAt(size_t index) const override { return local_change<V>([&]() { return list.removeAt(index); }); }

    V const &get(size_t index) const override { return list.get(index); };

    V set(size_t index, V element) const override {
        return local_change<V>([&]() { return list.set(index, std::move(element)); });
    }

    void clear() const override { return local_change([&]() { list.clear(); }); }

    size_t size() const override { return list.size(); }

    bool empty() const override { return list.empty(); }

    std::vector<std::shared_ptr<V> > const &getList() const override { return list.getList(); }

    bool addAll(size_t index, std::vector<V> elements) const override {
        return local_change<bool>([&]() mutable { return list.addAll(index, std::move(elements)); });
    }

    bool addAll(std::vector<V> elements) const override {
        return local_change<bool>([&]() mutable { return list.addAll(std::move(elements)); });
    }

    bool removeAll(std::vector<V> elements) const override {
        return local_change<bool>([&]() mutable { return list.removeAll(std::move(elements)); });
    }
};

static_assert(std::is_move_constructible_v<RdList<int> >, "Is move constructible RdList<int>");

#endif //RD_CPP_RDLIST_H

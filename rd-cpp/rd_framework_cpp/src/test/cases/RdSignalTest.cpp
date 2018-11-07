#include <gtest/gtest.h>

#include "RdBindableBase.h"
#include "RdSignal.h"
#include "RdFrameworkTestBase.h"

using vi = std::vector<int>;

TEST_F(RdFrameworkTestBase, signal_statics) {
    int signal_id = 1;

    RdSignal<int> client_signal_storage;
    RdSignal<int> server_signal_storage;

    RdSignal<int> &client_signal = statics(client_signal_storage, signal_id);
    RdSignal<int> &server_signal = statics(server_signal_storage, signal_id);

    std::vector<int> client_log;
    std::vector<int> server_log;

    client_signal.advise(Lifetime::Eternal(), [&client_log](int v) { client_log.push_back(v); });
    server_signal.advise(Lifetime::Eternal(), [&server_log](int v) { server_log.push_back(v); });

    //not bound
    EXPECT_EQ((vi{}), client_log);
    EXPECT_EQ((vi{}), client_log);

    EXPECT_THROW(client_signal.fire(2), std::exception);
    EXPECT_THROW(server_signal.fire(2), std::exception);

    //bound
    bindStatic(serverProtocol.get(), server_signal, "top");
    bindStatic(clientProtocol.get(), client_signal, "top");

    //set from client
    client_signal.fire(2);
    EXPECT_EQ((vi{2}), client_log);
    EXPECT_EQ((vi{2}), server_log);

    //set from client
    server_signal.fire(3);
    EXPECT_EQ((vi{2, 3}), client_log);
    EXPECT_EQ((vi{2, 3}), server_log);

    AfterTest();
}

TEST_F(RdFrameworkTestBase, signal_void_statics) {
    int signal_id = 1;

    RdSignal<void *> client_signal_storage;
    RdSignal<void *> server_signal_storage;

    RdSignal<void *> &client_signal = bindStatic(clientProtocol.get(), statics(client_signal_storage, signal_id),
                                                 "top");
    RdSignal<void *> &server_signal = bindStatic(serverProtocol.get(), statics(server_signal_storage, signal_id),
                                                 "top");

    int acc = 0;
    Lifetime::use([&](Lifetime lt) {
        server_signal.advise(lt, [&acc](void *) { acc++; });
        EXPECT_EQ(0, acc);

        client_signal.fire(nullptr);
        EXPECT_EQ(1, acc);

        client_signal.fire(nullptr);
        EXPECT_EQ(2, acc);
    });

    client_signal.fire(nullptr); //no transmitting
    EXPECT_EQ(2, acc);

    AfterTest();
}

class CustomSerializer {
public:
    static int32_t read(SerializationCtx const &ctx, Buffer const &buffer) {
        bool negate = buffer.read_pod<bool>();
        int32_t module = buffer.read_pod<int32_t>();
        return negate ? -module : module;
    }

    static void write(SerializationCtx const &ctx, Buffer const &buffer, const int &value) {
        buffer.write_pod(value < 0);
        buffer.write_pod(abs(value));
    }
};


TEST_F(RdFrameworkTestBase, signal_custom_serializer) {
    int signal_id = 1;

    RdSignal<int32_t, CustomSerializer> client_signal_storage;
    RdSignal<int32_t, CustomSerializer> server_signal_storage;

    RdSignal<int32_t, CustomSerializer> &client_signal = statics(client_signal_storage, signal_id);
    RdSignal<int32_t, CustomSerializer> &server_signal = statics(server_signal_storage, signal_id);

    int32_t client_log;
    int32_t server_log;

    client_signal.advise(Lifetime::Eternal(), [&client_log](int32_t v) { client_log = v; });
    server_signal.advise(Lifetime::Eternal(), [&server_log](int32_t v) { server_log = v; });

    bindStatic(serverProtocol.get(), server_signal, "top");
    bindStatic(clientProtocol.get(), client_signal, "top");

    //set from client
    client_signal.fire(2);
    EXPECT_EQ(2, client_log);
    EXPECT_EQ(2, server_log);

    //set from client
    server_signal.fire(-3);
    EXPECT_EQ(-3, client_log);
    EXPECT_EQ(-3, server_log);

    AfterTest();
}

template<typename K>
class Foo : public ISerializable {
    K x, y;
public:
    //region ctor/dtor

    explicit Foo(K x = 0, K y = 0) : x(x), y(y) {};

    Foo(Foo const &) = delete;

    Foo(Foo &&other) noexcept = default;

    Foo &operator=(Foo const &) = default;

    Foo &operator=(Foo &&) noexcept = default;

    virtual ~Foo() = default;
    //endregion

    static void registry(IProtocol *protocol) {
        protocol->serializers.registry<Foo>([](SerializationCtx const &ctx, Buffer const &buffer) {
            K x = buffer.read_pod<K>();
            K y = buffer.read_pod<K>();
            return Foo(x, y);
        });
    }

    void write(SerializationCtx const &ctx, Buffer const &buffer) const override {
        buffer.write_pod(x);
        buffer.write_pod(y);
    }

    friend bool operator==(const Foo &lhs, const Foo &rhs) {
        return lhs.x == rhs.x &&
               lhs.y == rhs.y;
    }

    friend bool operator!=(const Foo &lhs, const Foo &rhs) {
        return !(rhs == lhs);
    }
};

TEST_F(RdFrameworkTestBase, signal_custom_iserializable) {
    Foo<char>::registry(clientProtocol.get());
    Foo<char>::registry(serverProtocol.get());

    int signal_id = 1;

    RdSignal<Foo<char>> client_signal_storage;
    RdSignal<Foo<char>> server_signal_storage;

    RdSignal<Foo<char>> &client_signal = statics(client_signal_storage, signal_id);
    RdSignal<Foo<char>> &server_signal = statics(server_signal_storage, signal_id);

    Foo<char> client_log;
    Foo<char> server_log;

    client_signal.advise(Lifetime::Eternal(), [&client_log](Foo<char> const &v) { client_log = v; });
    server_signal.advise(Lifetime::Eternal(), [&server_log](Foo<char> const &v) { server_log = v; });

    bindStatic(serverProtocol.get(), server_signal, "top");
    bindStatic(clientProtocol.get(), client_signal, "top");

    //set from client
    client_signal.fire(Foo<char>(2, 0));
    EXPECT_EQ(Foo<char>(2, 0), client_log);
    EXPECT_EQ(Foo<char>(2, 0), server_log);

    //set from client
    server_signal.fire(Foo<char>(1, 8));
    EXPECT_EQ((Foo<char>(1, 8)), client_log);
    EXPECT_EQ((Foo<char>(1, 8)), server_log);

    AfterTest();
}

TEST_F(RdFrameworkTestBase, signal_vector) {
    int signal_id = 1;

    using array = std::vector<int>;

    RdSignal<array> client_signal_storage;
    RdSignal<array> server_signal_storage;

    RdSignal<array> &client_signal = statics(client_signal_storage, signal_id);
    RdSignal<array> &server_signal = statics(server_signal_storage, signal_id);

    array client_log;
    array server_log;

    client_signal.advise(Lifetime::Eternal(), [&client_log](array v) { client_log = v; });
    server_signal.advise(Lifetime::Eternal(), [&server_log](array v) { server_log = v; });

    bindStatic(serverProtocol.get(), server_signal, "top");
    bindStatic(clientProtocol.get(), client_signal, "top");

    //set from client
    array a{2, 0, 1, 8};
    client_signal.fire(a);
    EXPECT_EQ(a, client_log);
    EXPECT_EQ(a, server_log);

    //set from client
    a = {8, 8, 9, 8, 8, 0, 2, 1, 8, 6, 0};
    server_signal.fire(a);
    EXPECT_EQ(a, client_log);
    EXPECT_EQ(a, server_log);

    AfterTest();
}
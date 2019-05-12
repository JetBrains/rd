#ifndef RD_CPP_INTERNINGTESTBASE_H
#define RD_CPP_INTERNINGTESTBASE_H

#include "RdFrameworkTestBase.h"

#include "RdProperty.h"

#include <functional>
#include <numeric>

namespace rd {
    namespace test {
        class InterningTestBase : public RdFrameworkTestBase {
        protected:

            const std::vector<std::pair<int32_t, std::wstring>> simpleTestData{
                {0, L""},
                {1, L"test"},
                {2, L"why"}
            };

            template <typename W, typename T>
            int64_t get_written_bytes_count(IProtocol* protocol, T& sender_model_view,
                                            int32_t offset, const std::wstring &suffix) {
                return measureBytes(protocol, [&]() {
                    for_each([&](int32_t const& k, std::wstring const& v) {
                        sender_model_view.get_issues().set(k + offset, W(v + suffix));
                    });
                });
            }

            static int64_t measureBytes(IProtocol* protocol, std::function<void()> action);

            void for_each(std::function<void(int32_t, std::wstring)> f);

            template <typename Model, typename String>
            void doTest(bool firstClient, bool secondClient, bool thenSwitchSides = false) {
                RdProperty<Model> server_property;
                RdProperty<Model> client_property;
                statics(server_property, 1).slave();
                statics(client_property, 1);

                bindStatic(serverProtocol.get(), server_property, "top");
                bindStatic(clientProtocol.get(), client_property, "top");

                server_property.set(Model(L""));
                auto const& server_model_view = server_property.get();

                auto const& client_model_view = client_property.get();

                IProtocol* firstSenderProtocol = (firstClient) ? clientProtocol.get() : serverProtocol.get();
                auto const& first_sender_model_view = (firstClient) ? client_model_view : server_model_view;

                auto first_bytes_written = get_written_bytes_count<String>(
                    firstSenderProtocol, first_sender_model_view, 0, L"");

                IProtocol* secondSenderProtocol = (secondClient) ? clientProtocol.get() : serverProtocol.get();
                auto const& second_sender_model_view = (secondClient) ? client_model_view : server_model_view;

                auto second_bytes_written = get_written_bytes_count<String>(
                    secondSenderProtocol, second_sender_model_view,
                    simpleTestData.size(), L"");

                int64_t sum12 = std::accumulate(simpleTestData.begin(), simpleTestData.end(), 0L,
                                              [&](int64_t acc, std::pair<int32_t, std::wstring> it) {
                                                  return acc += it.second.length();
                                              });
//                std::cerr << first_bytes_written << " " << second_bytes_written << std::endl;
                EXPECT_TRUE(first_bytes_written - sum12 >= second_bytes_written);

                auto const& first_receiver_view = (firstClient) ? server_model_view : client_model_view;
                auto const& second_receiver_view = (secondClient) ? server_model_view : client_model_view;

                for_each([&](int32_t const& k, std::wstring const& v) {
                    auto value = first_receiver_view.get_issues().get(k);
                    EXPECT_NE(nullptr, value);
                    EXPECT_EQ(v, value->get_text());

                    value = second_receiver_view.get_issues().get(k + simpleTestData.size());
                    EXPECT_NE(nullptr, value);
                    EXPECT_EQ(v, value->get_text());
                });

                if (!thenSwitchSides) {
                    AfterTest();
                    return;
                }

                std::wstring extraString = L"again";

                auto third_bytes_written = get_written_bytes_count<String>(
                    secondSenderProtocol, second_sender_model_view,
                    simpleTestData.size() * 2, extraString);

                auto fourth_bytes_written = get_written_bytes_count<String>(
                    firstSenderProtocol, first_sender_model_view,
                    simpleTestData.size() * 3, extraString);

				int64_t sum34 = std::accumulate(simpleTestData.begin(), simpleTestData.end(), 0L,
											  [&](int64_t acc, std::pair<int32_t, std::wstring> it) {
												  return acc += it.second.length() + extraString.length();
											  });

				EXPECT_TRUE(third_bytes_written - sum34 >= fourth_bytes_written);

                for_each([&](int32_t const& k, std::wstring const& v) {
                    EXPECT_EQ(v + extraString,
                              second_receiver_view.get_issues().get(k + simpleTestData.size() * 2)->get_text());
                    EXPECT_EQ(v + extraString,
                              first_receiver_view.get_issues().get(k + simpleTestData.size() * 3)->get_text());
                });

                AfterTest();
            }

            void testIntern(bool firstClient, bool secondClient, bool thenSwitchSides = false);
            
            void testProtocolLevelIntern(bool firstClient, bool secondClient, bool thenSwitchSides = false);

           
        };
    }
}


#endif //RD_CPP_INTERNINGTESTBASE_H

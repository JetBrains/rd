#include "InterningTestBase.h"

#include "InterningProtocolLevelModel.h"

#include <numeric>

namespace rd {
	namespace test {
		using namespace util;

		int64_t InterningTestBase::measureBytes(IProtocol *protocol, std::function<void()> action) {
			auto wire = dynamic_cast<SimpleWire const *>(protocol->get_wire());
			auto pre = wire->bytesWritten;
			action();
			return wire->bytesWritten - pre;
		}

		int64_t InterningTestBase::get_written_bytes_count
				(IProtocol *protocol, const InterningTestModel &sender_model_view, int32_t offset, std::wstring suffix) {
			return measureBytes(protocol, [&]() {
				for_each([&](int32_t const &k, std::wstring const &v) {
					sender_model_view.get_issues().set(k + offset, WrappedStringModel(v + suffix));
				});
			});
		}

		void InterningTestBase::for_each(std::function<void(int32_t, std::wstring)> f) {
			std::for_each(simpleTestData.begin(), simpleTestData.end(), [&](auto const &p) {
				auto const &k = p.first;
				auto const &v = p.second;
				f(k, v);
			});
		}

		void InterningTestBase::doTest(bool firstClient, bool secondClient, bool thenSwitchSides) {
			RdProperty<InterningTestModel> server_property;
			RdProperty<InterningTestModel> client_property;
			statics(server_property, 1).slave();
			statics(client_property, 1);

			bindStatic(serverProtocol.get(), server_property, "top");
			bindStatic(clientProtocol.get(), client_property, "top");

			server_property.set(InterningTestModel(L""));
			auto const &server_model_view = server_property.get();

			auto const &client_model_view = client_property.get();

			IProtocol *firstSenderProtocol = (firstClient) ? clientProtocol.get() : serverProtocol.get();
			auto const &first_sender_model_view = (firstClient) ? client_model_view : server_model_view;

			auto first_bytes_written = get_written_bytes_count(firstSenderProtocol, first_sender_model_view, 0, L"");

			IProtocol *secondSenderProtocol = (secondClient) ? clientProtocol.get() : serverProtocol.get();
			auto const &second_sender_model_view = (secondClient) ? client_model_view : server_model_view;

			auto second_bytes_written = get_written_bytes_count(secondSenderProtocol, second_sender_model_view,
																simpleTestData.size(), L"");

			int64_t sum = std::accumulate(simpleTestData.begin(), simpleTestData.end(), 0L,
										  [&](int64_t acc, std::pair<int32_t, std::wstring> it) {
											  return acc += it.second.length();
										  });
			std::cerr << first_bytes_written << " " << second_bytes_written << std::endl;
			EXPECT_TRUE(first_bytes_written - sum >= second_bytes_written);

			auto const &first_receiver_view = (firstClient) ? server_model_view : client_model_view;
			auto const &second_receiver_view = (secondClient) ? server_model_view : client_model_view;

			for_each([&](int32_t const &k, std::wstring const &v) {
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

			auto extraString = L"again";

			auto third_bytes_written = get_written_bytes_count(secondSenderProtocol, second_sender_model_view,
															   simpleTestData.size() * 2, extraString);

			auto fourth_bytes_written = get_written_bytes_count(firstSenderProtocol, first_sender_model_view,
																simpleTestData.size() * 3, extraString);

			for_each([&](int32_t const &k, std::wstring const &v) {
				EXPECT_EQ(v + extraString,
						  second_receiver_view.get_issues().get(k + simpleTestData.size() * 2)->get_text());
				EXPECT_EQ(v + extraString,
						  first_receiver_view.get_issues().get(k + simpleTestData.size() * 3)->get_text());
			});

			AfterTest();
		}

		void InterningTestBase::testProtocolLevelIntern(bool firstClient, bool secondClient, bool thenSwitchSides) {
			
			AfterTest();
		}
	}
}

#ifndef RD_CPP_CROSSTESTBASE_H
#define RD_CPP_CROSSTESTBASE_H


#include <fstream>
#include "LifetimeDefinition.h"
#include "SimpleScheduler.h"
#include "IWire.h"
#include "SocketWire.h"
#include "filesystem.h"
#include "Protocol.h"
#include "WireUtil.h"

namespace rd {
	namespace cross {
		class CrossTestBase {
		protected:
			using printer_t = std::vector<std::string>;

			printer_t printer;

			rd::SimpleScheduler scheduler{};

			rd::LifetimeDefinition definition{false};
			rd::Lifetime model_lifetime = definition.lifetime;

			rd::LifetimeDefinition socket_definition{false};
			rd::Lifetime socket_lifetime = definition.lifetime;

			static const std::string port_file;
			static const std::string port_file_closed;

			std::shared_ptr<rd::IWire> wire;
			std::unique_ptr<rd::IProtocol> protocol;

			std::ofstream out;

			std::promise<void> promise;
			std::future<void> f = promise.get_future();
		public:
			CrossTestBase();

			int main(int argc, char **argv, const std::string &test_name) {
				--argc;
				if (argc != 1) {
					std::cerr << "Wrong number of arguments for " << test_name << ":" << argc << ", expected = 1. "
																								 "main([\"CrossTestClientAllEntities\"]) for example.)";
					exit(1);
				}
				const auto output_file = std::string(argv[1]);
				std::cerr << "Test:" << test_name << " started, file=" << output_file << std::endl;

				out = std::ofstream(output_file);
				return run();
			}

		protected:

			virtual int run() = 0;

			void terminate() {
				auto status = f.wait_for(std::chrono::seconds(10));
				rd::util::sleep_this_thread(1'000);

				socket_definition.terminate();
				definition.terminate();

				for (const auto &item : printer) {
					out << item << std::endl;
				}
				out.close();
			}
		};
	}
}

#endif //RD_CPP_CROSSTESTBASE_H

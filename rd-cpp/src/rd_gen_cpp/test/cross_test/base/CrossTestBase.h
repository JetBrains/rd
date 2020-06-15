#ifndef RD_CPP_CROSSTESTBASE_H
#define RD_CPP_CROSSTESTBASE_H

#include "lifetime/LifetimeDefinition.h"
#include "scheduler/SimpleScheduler.h"
#include "base/IWire.h"
#include "wire/SocketWire.h"
#include "std/filesystem.h"
#include "protocol/Protocol.h"
#include "wire/WireUtil.h"

#include <fstream>

namespace rd
{
namespace cross
{
class CrossTestBase
{
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

public:
	CrossTestBase();

	int main(int argc, char** argv, const std::string& test_name)
	{
		--argc;
		if (argc != 1)
		{
			std::cerr << "Wrong number of arguments for " << test_name << ":" << argc
					  << ", expected = 1. "
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

	static constexpr int SPINNING_TIMEOUT = 10'000;

	void terminate()
	{
		rd::util::sleep_this_thread(SPINNING_TIMEOUT);

		socket_definition.terminate();
		definition.terminate();

		for (const auto& item : printer)
		{
			out << item << std::endl;
		}
		out.close();
	}
};
}	 // namespace cross
}	 // namespace rd

#endif	  // RD_CPP_CROSSTESTBASE_H

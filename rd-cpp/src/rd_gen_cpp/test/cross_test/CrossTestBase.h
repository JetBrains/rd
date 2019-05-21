#ifndef RD_CPP_CROSSTESTBASE_H
#define RD_CPP_CROSSTESTBASE_H


#include <fstream>
#include "LifetimeDefinition.h"
#include "SimpleScheduler.h"
#include "IWire.h"
#include "SocketWire.h"
#include "filesystem.h"
#include "Protocol.h"

class CrossTestBase {
protected:
	using printer_t = std::vector<std::string>;

	printer_t printer;

	rd::SimpleScheduler scheduler{};

	rd::LifetimeDefinition definition{false};
	rd::Lifetime lifetime = definition.lifetime;

	rd::LifetimeDefinition socket_definition{false};
	rd::Lifetime socket_lifetime = definition.lifetime;

	static const std::string tmp_directory;

	std::shared_ptr<rd::IWire> wire;
	std::unique_ptr<rd::IProtocol> protocol;


public:
	CrossTestBase();

protected:
	void terminate() {
		socket_definition.terminate();
		definition.terminate();
	}
};


#endif //RD_CPP_CROSSTESTBASE_H

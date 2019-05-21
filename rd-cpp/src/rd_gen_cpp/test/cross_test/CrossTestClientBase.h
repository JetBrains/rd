#ifndef RD_CPP_CROSSTESTCLIENTBASE_H
#define RD_CPP_CROSSTESTCLIENTBASE_H

#include "CrossTestBase.h"

#include "LifetimeDefinition.h"
#include "SimpleScheduler.h"
#include "IWire.h"
#include "SocketWire.h"
#include "filesystem.h"
#include "Protocol.h"

#include <fstream>

class CrossTestClientBase : public CrossTestBase {
private:
	void
	printIfRemoteChangeImpl(printer_t &printer, rd::RdReactiveBase const &entity, std::string const &entity_name) {}

	template<typename T, class... Ts>
	void printIfRemoteChangeImpl(printer_t &printer, rd::RdReactiveBase const &entity, std::string const &entity_name,
								 T &&arg0,
								 Ts &&...args) {
		print(printer, arg0);
		printIfRemoteChangeImpl(printer, entity, entity_name, std::forward<Ts>(args)...);
	}

protected:
	template<class... Ts>
	void
	printIfRemoteChange(printer_t &printer, rd::RdReactiveBase const &entity, std::string entity_name, Ts &&...args) {
		if (!entity.is_local_change) {
			print(printer, "***");
			print(printer, entity_name + ':');
			printIfRemoteChangeImpl(printer, entity, entity_name, std::forward<Ts>(args)...);
		}
	}


	CrossTestClientBase();

	template<typename T>
	void print(printer_t &printer, T const &x) {
		printer.push_back(rd::to_string(x));
	}

};


#endif //RD_CPP_CROSSTESTCLIENTBASE_H

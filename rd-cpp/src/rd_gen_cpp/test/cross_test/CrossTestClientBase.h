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
	template<typename T>
	void printIfRemoteChangeImpl(printer_t &printer, rd::ISource<T> const &entity, std::string const &entity_name) {}

	template<typename T, typename T0, class... Ts>
	void printIfRemoteChangeImpl(printer_t &printer, rd::ISource<T> const &entity, std::string const &entity_name,
								 T0 &&arg0,
								 Ts &&...args) {
		print(printer, arg0);
		printIfRemoteChangeImpl(printer, entity, entity_name, std::forward<Ts>(args)...);
	}

protected:
	template <typename T>
	bool is_local_change_of(rd::ISource<T> const& source) {
		try {
			return dynamic_cast<rd::RdReactiveBase const &>(source).is_local_change;
		} catch (std::bad_cast const &) {
			return false;
		}
	}

	template<typename T, class... Ts>
	void
	printIfRemoteChange(printer_t &printer, rd::ISource<T> const &entity, std::string entity_name, Ts &&...args) {
		if (!is_local_change_of(entity)) {
			print(printer, "***");
			print(printer, entity_name + ':');
			printIfRemoteChangeImpl(printer, entity, entity_name, std::forward<Ts>(args)...);
		}
	}

	/*template<typename T, class... Ts>
	void
	printIfRemoteChange(printer_t &printer, rd::IViewable<T> const &entity, std::string entity_name, Ts &&...args) {
		if (!dynamic_cast<rd::RdReactiveBase const &>(entity).is_local_change) {
			print(printer, "***");
			print(printer, entity_name + ':');
			printIfRemoteChangeImpl(printer, entity, entity_name, std::forward<Ts>(args)...);
		}
	}*/


	CrossTestClientBase();

	template<typename T>
	void print(printer_t &printer, T const &x) {
		printer.push_back(rd::to_string(x));
	}

};


#endif //RD_CPP_CROSSTESTCLIENTBASE_H

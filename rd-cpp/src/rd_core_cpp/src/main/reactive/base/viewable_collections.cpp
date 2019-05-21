#include "viewable_collections.h"

namespace rd {
	std::string to_string(AddRemove kind) {
		if (kind == AddRemove::ADD) return "Add";
		if (kind == AddRemove::REMOVE) return "Remove";
		return "";
	}

	std::string to_string(Op op) {
		if (op == Op::ADD) return "Add";
		if (op == Op::REMOVE) return "Remove";
		if (op == Op::UPDATE) return "Update";
		if (op == Op::ACK) return "Ack";
		return "";
	}
}

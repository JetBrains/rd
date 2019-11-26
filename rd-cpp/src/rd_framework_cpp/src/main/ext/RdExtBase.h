#ifndef RD_CPP_RDEXTBASE_H
#define RD_CPP_RDEXTBASE_H

#include "base/RdReactiveBase.h"
#include "ExtWire.h"

#pragma warning( push )
#pragma warning( disable:4250 )
namespace rd {
	/**
	 * \brief Base class for creating extension node according to bottom-up design.  
	 */
	class RdExtBase : public RdReactiveBase {
		std::shared_ptr<ExtWire> extWire = std::make_shared<ExtWire>();
		mutable std::shared_ptr<IProtocol> extProtocol/* = nullptr*/;
	public:
		enum class ExtState {
			Ready,
			ReceivedCounterpart,
			Disconnected
		};

		//region ctor/dtor

		RdExtBase() = default;

		RdExtBase(RdExtBase &&) = default;

		RdExtBase &operator=(RdExtBase &&) = default;

		virtual ~RdExtBase() = default;
		//endregion

		mutable int64_t serializationHash = 0;

		const IProtocol *get_protocol() const override;

		IScheduler *get_wire_scheduler() const override;

		void init(Lifetime lifetime) const override;

		void on_wire_received(Buffer buffer) const override;

		void sendState(IWire const &wire, ExtState state) const;

		void traceMe(const Logger &logger, string_view message) const;
	};

	std::string to_string(RdExtBase::ExtState state);
}

#pragma warning( pop )

#endif //RD_CPP_RDEXTBASE_H

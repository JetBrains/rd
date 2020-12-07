#ifndef RD_CPP_FRAMEWORK_IRDREACTIVE_H
#define RD_CPP_FRAMEWORK_IRDREACTIVE_H

#include "IRdBindable.h"

#include <protocol/Buffer.h>

#include <rd_framework_export.h>

namespace rd
{
class IScheduler;

/**
 * \brief A non-root node in an object graph which can be synchronized with its remote copy over a network or
 * a similar connection, and which allows to subscribe to its changes.
 */
class RD_FRAMEWORK_API IRdReactive : public virtual IRdBindable
{
public:
	/**
	 * \brief If set to true, local changes to this object can be performed on any thread.
	 * Otherwise, local changes can be performed only on the UI thread.
	 */
	bool async = false;
	// region ctor/dtor

	IRdReactive() = default;

	~IRdReactive() override = default;
	// endregion

	/**
	 * \brief Scheduler on which wire invokes callback [onWireReceived]. Default is the same as [protocol]'s one.
	 * \return scheduler
	 */
	virtual IScheduler* get_wire_scheduler() const = 0;

	/**
	 * \brief Callback that wire triggers when it receives messaged
	 * \param buffer where serialised info is stored
	 */
	virtual void on_wire_received(Buffer buffer) const = 0;
};
}	 // namespace rd

#endif	  // RD_CPP_FRAMEWORK_IRDREACTIVE_H

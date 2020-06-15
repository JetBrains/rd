#include "SingleThreadScheduler.h"

#include <utility>

#include "ctpl_stl.h"

namespace rd
{
SingleThreadScheduler::SingleThreadScheduler(Lifetime lifetime, std::string name)
	: SingleThreadSchedulerBase(std::move(name)), lifetime(lifetime)
{
	lifetime->add_action([this]() {
		try
		{
			pool->stop(true);
		}
		catch (std::exception const& e)
		{
			log.error("Failed to terminate %s", this->name.c_str());
		}
	});
}
}	 // namespace rd
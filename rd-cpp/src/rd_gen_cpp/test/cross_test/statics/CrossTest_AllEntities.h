#ifndef RD_CPP_CROSSTESTALLENTITIES_H
#define RD_CPP_CROSSTESTALLENTITIES_H

#include "DemoModel/DemoModel.Generated.h"
#include "ExtModel/ExtModel.Generated.h"

#include <string>
#include <vector>
#include <future>

namespace rd
{
namespace cross
{
class CrossTestAllEntities
{
	using printer_t = std::vector<std::string>;

public:
	static void fireAll(const demo::DemoModel& model, const demo::ExtModel& extModel);
};
}	 // namespace cross
}	 // namespace rd

#endif	  // RD_CPP_CROSSTESTALLENTITIES_H

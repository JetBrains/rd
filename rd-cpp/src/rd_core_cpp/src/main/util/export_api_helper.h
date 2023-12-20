#ifndef EXPORT_API_HELPER_H
#define EXPORT_API_HELPER_H

#if defined(_MSC_VER)
#define RD_PUSH_STL_EXPORTS_WARNINGS \
	_Pragma("warning(push)") \
	_Pragma("warning(disable:4251)")
#define RD_POP_STL_EXPORTS_WARNINGS \
	_Pragma("warning(pop)")
#else
#define RD_PUSH_STL_EXPORTS_WARNINGS
#define RD_POP_STL_EXPORTS_WARNINGS
#endif

#endif //EXPORT_API_HELPER_H

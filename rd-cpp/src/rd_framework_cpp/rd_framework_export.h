
#ifndef RD_FRAMEWORK_API_H
#define RD_FRAMEWORK_API_H

#ifdef RD_FRAMEWORK_STATIC_DEFINE
#  define RD_FRAMEWORK_API
#  define RD_FRAMEWORK_NO_EXPORT
#else
#  ifndef RD_FRAMEWORK_API
#    ifdef rd_framework_cpp_EXPORTS
        /* We are building this library */
#      define RD_FRAMEWORK_API __declspec(dllexport)
#    else
        /* We are using this library */
#      define RD_FRAMEWORK_API __declspec(dllimport)
#    endif
#  endif

#  ifndef RD_FRAMEWORK_NO_EXPORT
#    define RD_FRAMEWORK_NO_EXPORT 
#  endif
#endif

#ifndef RD_FRAMEWORK_DEPRECATED
#  define RD_FRAMEWORK_DEPRECATED __declspec(deprecated)
#endif

#ifndef RD_FRAMEWORK_DEPRECATED_EXPORT
#  define RD_FRAMEWORK_DEPRECATED_EXPORT RD_FRAMEWORK_API RD_FRAMEWORK_DEPRECATED
#endif

#ifndef RD_FRAMEWORK_DEPRECATED_NO_EXPORT
#  define RD_FRAMEWORK_DEPRECATED_NO_EXPORT RD_FRAMEWORK_NO_EXPORT RD_FRAMEWORK_DEPRECATED
#endif

#if 0 /* DEFINE_NO_DEPRECATED */
#  ifndef RD_FRAMEWORK_NO_DEPRECATED
#    define RD_FRAMEWORK_NO_DEPRECATED
#  endif
#endif

#endif /* RD_FRAMEWORK_API_H */

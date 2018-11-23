//
// Created by jetbrains on 01.11.2018.
//

#include "RdTaskResult.h"

template<typename T, typename S>
RdTaskResult<T, S>::Fault::Fault(std::wstring reasonTypeFqn, std::wstring reasonMessage,
                                 std::wstring reasonAsText):reasonTypeFqn(std::move(reasonTypeFqn)),
                                                            reasonMessage(std::move(reasonMessage)),
                                                            reasonAsText(std::move(reasonAsText)) {}

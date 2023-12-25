#ifndef SIMPLESOCKETSENDER_H
#define SIMPLESOCKETSENDER_H

#include <memory>

#include "SimpleSocket.h"

class CSimpleSocketSender
{
    std::shared_ptr<CSimpleSocket> m_socket;
    CSimpleSocket::CSocketError m_error;

public:
    explicit CSimpleSocketSender(const std::shared_ptr<CSimpleSocket>& socket) : m_error(CSimpleSocket::SocketSuccess)
    {
        if (socket->m_nSocketType == CSimpleSocket::CSocketType::SocketTypeTcp)
            m_socket = socket;
        else
            m_socket = std::make_shared<CSimpleSocket>();
    }

    int32_t Send(const uint8_t* pBuf, size_t bytesToSend) const;

    bool IsSocketValid() const
    {
        return m_socket->IsSocketValid();
    }

    CSimpleSocket::CSocketError GetSocketError() const
    {
        return m_error;
    }

    const char* DescribeError() const
    {
        return CSimpleSocket::DescribeError(m_error);
    }
};

#endif	  // SIMPLESOCKETSENDER_H

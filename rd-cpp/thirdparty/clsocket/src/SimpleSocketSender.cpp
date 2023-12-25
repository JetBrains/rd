//
// Created by Alexander.Bondarev on 25/12/2023.
//

#include "SimpleSocketSender.h"

int32_t CSimpleSocketSender::Send(const uint8_t *pBuf, const size_t bytesToSend) const
{
    int32_t bytesSent = 0;
    if (m_socket->IsSocketValid())
    {
        if ((bytesToSend > 0) && (pBuf != nullptr))
        {
            //---------------------------------------------------------
            // Check error condition and attempt to resend if call
            // was interrupted by a signal.
            //---------------------------------------------------------
            CSimpleSocket::CSocketError socket_error;
            do
            {
                bytesSent = static_cast<int32_t>(SEND(m_socket->m_socket, pBuf, bytesToSend, 0));
                socket_error = CSimpleSocket::TranslateLastSocketError();
            } while (socket_error == CSimpleSocket::SocketInterrupted);
        }
    }

    return bytesSent;
}

package tj.mirzodaler.standby.media

import android.service.notification.NotificationListenerService

/**
 * Пустой слушатель уведомлений. Нужен только как «ключ»: пока пользователь дал ему доступ,
 * Android разрешает читать активные медиасессии (Spotify, YouTube Music, любой плеер)
 * и управлять ими. Сами уведомления не читаем и никуда не отправляем.
 */
class MediaListener : NotificationListenerService()

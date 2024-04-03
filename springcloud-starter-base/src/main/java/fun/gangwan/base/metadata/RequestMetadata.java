package fun.gangwan.base.metadata;

import lombok.*;


/**
 *
 *
 * <br>API Header基础数据结构</br>
 *
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RequestMetadata {

    private String uri;

    private String method;

    private String contentType;

    private String requestId;

    private long start;

    //**************************

    private String clientHostName;

    private String clientIp;

    //*********** Basic Auth ***************

    private String clientName;

    private String secret;

}

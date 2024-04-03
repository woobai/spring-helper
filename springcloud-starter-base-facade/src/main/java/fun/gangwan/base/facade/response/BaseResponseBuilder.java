package fun.gangwan.base.facade.response;

import com.github.pagehelper.PageInfo;
import fun.gangwan.base.facade.constant.MDCConstant;
import fun.gangwan.base.facade.enums.BizEnum;
import fun.gangwan.base.facade.enums.BizErrorCodeEnum;
import fun.gangwan.base.facade.extend.orika.ConfigurableMapperFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * API通用返回结构工厂类 BaseResponseBuilder
 *
 */
public class BaseResponseBuilder {

    /**
     *
     * @return 返回success
     */
    public static BaseResponse<String> success() {
        return build(BizErrorCodeEnum.SUCCESS, null, null,false);
    }

    /**
     *
     * @param msg
     * @return 返回success 并覆盖默认message
     */
    public static BaseResponse<String> success(String msg) {
        return build(BizErrorCodeEnum.SUCCESS, msg, null, false);
    }

    /**
     *
     * @param data
     * @return 返回success状态 data为字符串
     */
    public static BaseResponse<String> successString(String data) {
        return build(BizErrorCodeEnum.SUCCESS, data, null);
    }

    /**
     *
     * @return 返回failed
     */
    public static BaseResponse<String> failed() {
        return build(BizErrorCodeEnum.OPERATION_FAILED, null, null, true);
    }

    /**
     *
     * @return 返回failed 并覆盖默认message
     */
    public static BaseResponse<String> failed(String msg, String detail) {
        return build(BizErrorCodeEnum.OPERATION_FAILED, msg, detail, true);
    }


    public static BaseResponse<String> failed(int code, String msg, String detail, boolean trace) {
        return buildString(code, msg, detail, trace);
    }

    /**
     *
     * @return 返回failed 并覆盖默认message
     */
    public static BaseResponse<String> failed(BizEnum bizEnum, String msg) {
        return buildString(bizEnum.getCode(), msg, null, true);
    }

    /**
     *
     * @return 返回failed
     */
    public static BaseResponse<String> failed(BizEnum bizEnum) {
        return buildString(bizEnum.getCode(), bizEnum.getDesc(), null, true);
    }

    /**
     * 构建返回对象
     *
     * @param bizEnum 返回code
     * @return 返回对象
     */
    public static <ResponseBean> BaseResponse<ResponseBean> build(BizEnum bizEnum) {
        return build(bizEnum, bizEnum.getDesc(), null, null);
    }

    /**
     * 构建返回对象
     *
     * @param bizEnum 返回code
     * @param msg     返回消息
     * @return 返回对象
     */
    public static <ResponseBean> BaseResponse<ResponseBean> build(BizEnum bizEnum, String msg) {
        return build(bizEnum, msg, null, null);
    }

    /**
     * 构建返回对象
     *
     * @param bizEnum      返回code
     * @param responseBean 返回data
     * @return 返回对象
     */
    public static <ResponseBean> BaseResponse<ResponseBean> buildData(BizEnum bizEnum, ResponseBean responseBean) {
        return build(bizEnum, null, responseBean, null);
    }

    /**
     * 构建返回对象
     *
     * @param bizEnum      返回code
     * @param msg          返回消息
     * @param responseBean 返回data
     * @return 返回对象
     */
    public static <ResponseBean> BaseResponse<ResponseBean> buildData(BizEnum bizEnum, String msg, ResponseBean responseBean) {
        return build(bizEnum, msg, responseBean, null);
    }

    /**
     *
     * @param bizEnum
     * @param msg
     * @return
     */
    private static BaseResponse<String> build(BizEnum bizEnum, String msg, String detail, boolean trace){
        return buildString(bizEnum.getCode(), StringUtils.isBlank(msg) ? bizEnum.getDesc() : msg, detail, trace);
    }

    /**
     *
     * @param code
     * @param msg
     * @param detail
     * @param trace
     * @return
     */
    private static StringResponse buildString(int code, String msg, String detail, boolean trace){
        StringResponse stringResponse = new StringResponse();
        stringResponse.setCode(code);
        stringResponse.setMessage(msg);
        stringResponse.setDetail(detail);
        if(trace){
            String traceNo = MDC.get(MDCConstant.REQUEST_ID);
            stringResponse.setTrace(traceNo);
        }
        return stringResponse;
    }

    /**
     *
     * @param bizEnum
     * @param responseBean
     * @param trace
     * @param <ResponseBean>
     * @return
     */
    private static <ResponseBean> BaseResponse<ResponseBean> build(BizEnum bizEnum, ResponseBean responseBean, String trace){
        BaseResponse<ResponseBean> baseResponse = new BaseResponse<>();
        baseResponse.setCode(bizEnum.getCode());
        baseResponse.setMessage(bizEnum.getDesc());
        baseResponse.setData(responseBean);
        baseResponse.setTrace(trace);
        return baseResponse;
    }

    /**
     *
     * @param bizEnum
     * @param responseBean
     * @param <ResponseBean>
     * @return
     */
    private static <ResponseBean> BaseResponse<ResponseBean> build(BizEnum bizEnum, String msg, ResponseBean responseBean, String trace){
        BaseResponse<ResponseBean> baseResponse = new BaseResponse<>();
        baseResponse.setCode(bizEnum.getCode());
        baseResponse.setMessage(StringUtils.isBlank(msg) ? bizEnum.getDesc() : msg);
        baseResponse.setData(responseBean);
        baseResponse.setTrace(trace);
        return baseResponse;
    }

    /**
     *
     * @param responseBean
     * @param <ResponseBean>
     * @return 创建实体型返回结果
     */
    public static <ResponseBean> BaseResponse<ResponseBean> success(ResponseBean responseBean) {
        return build(BizErrorCodeEnum.SUCCESS, null, responseBean, null);
    }

    /**
     *
     * @param msg
     * @param responseBean
     * @param <ResponseBean>
     * @return
     */
    public static <ResponseBean> BaseResponse<ResponseBean> success(String msg, ResponseBean responseBean) {
        return build(BizErrorCodeEnum.SUCCESS, msg, responseBean, null);
    }

    /**
     * @param entity
     * @param responseBeanClass
     * @param <Entity>
     * @param <ResponseBean>
     * @return 创建实体型返回结果
     */
    public static <Entity, ResponseBean> BaseResponse<ResponseBean> success(Entity entity, Class<ResponseBean> responseBeanClass) {
        ResponseBean responseBean = ConfigurableMapperFactory.getBeanMapper().map(entity, responseBeanClass);
        return success(responseBean);
    }

    /**
     *
     * @param responseBeanList
     * @param <ResponseBean>
     * @return 创建列表型返回结果 data:list:[]结构
     */
    public static <ResponseBean> ListResponse<ResponseBean> success(List<ResponseBean> responseBeanList) {
        ListResponse<ResponseBean> listResponse = new ListResponse<>();
        listResponse.setCode(BizErrorCodeEnum.SUCCESS.getCode());
        listResponse.setMessage(BizErrorCodeEnum.SUCCESS.getDesc());

        ListBean<ResponseBean> listBean = new ListBean<>();
        listBean.setList(responseBeanList);
        listResponse.setData(listBean);
        return listResponse;
    }

    /**
     *
     * @param responseBeanList
     * @param <ResponseBean>
     * @return 创建列表型返回结果 data:[]结构
     */
    public static <ResponseBean> BaseResponse<List<ResponseBean>> successDataList(List<ResponseBean> responseBeanList) {
        BaseResponse<List<ResponseBean>> listResponse = new BaseResponse<>();
        listResponse.setCode(BizErrorCodeEnum.SUCCESS.getCode());
        listResponse.setMessage(BizErrorCodeEnum.SUCCESS.getDesc());
        listResponse.setData(responseBeanList);
        return listResponse;
    }

    /**
     *
     * @param entityList
     * @param responseBeanClass
     * @param <Entity>
     * @param <ResponseBean>
     * @return 创建列表型返回结果
     */
    public static <Entity, ResponseBean> ListResponse<ResponseBean> success(List<Entity> entityList, Class<ResponseBean> responseBeanClass) {
        List<ResponseBean> responseBeans = ConfigurableMapperFactory.getBeanMapper().mapAsList(entityList, responseBeanClass);
        return success(responseBeans);
    }

    /**
     *
     * @param listEntity
     * @param responseBeanClass
     * @param <Entity>
     * @param <ResponseBean>
     * @return 创建带有分页信息的返回结果
     */
    public static <Entity, ResponseBean> PageInfoResponse<ResponseBean> successPage(List<Entity> listEntity, Class<ResponseBean> responseBeanClass) {
        PageInfo<ResponseBean> responseBeanPageInfo = toTarget(listEntity, responseBeanClass);

        return successPage(responseBeanPageInfo);
    }

    /**
     * 创建带有分页信息的返回结果
     * @param pageInfoEntity
     * @param responseBeanClass
     * @param <Entity>
     * @param <ResponseBean>
     * @return
     */
    public static <Entity, ResponseBean> PageInfoResponse<ResponseBean> successPage(PageInfo<Entity> pageInfoEntity, Class<ResponseBean> responseBeanClass) {
        PageInfo<ResponseBean> responseBeanPageInfo = toTarget(pageInfoEntity, responseBeanClass);

        return successPage(responseBeanPageInfo);
    }

    /**
     * 创建带有分页信息的返回结果
     * @param responseBeanPageInfo
     * @param <ResponseBean>
     * @return
     */
    public static <ResponseBean> PageInfoResponse<ResponseBean> successPage(PageInfo<ResponseBean> responseBeanPageInfo) {
        PageInfoResponse<ResponseBean> baseResponse = new PageInfoResponse<>();
        baseResponse.setCode(BizErrorCodeEnum.SUCCESS.getCode());
        baseResponse.setMessage(BizErrorCodeEnum.SUCCESS.getDesc());
        baseResponse.setData(responseBeanPageInfo);

        return baseResponse;
    }

    /**
     * 假分页,不推荐
     * 创建带有分页信息的返回结果, 手动计算分页
     *
     * @param allList
     * @param <ResponseBean>
     * @return
     */
    public static <ResponseBean> PageInfoResponse<ResponseBean> successPage(List<ResponseBean> allList, Integer pageNum, Integer pageSize) {
        PageInfoResponse<ResponseBean> baseResponse = new PageInfoResponse<>();
        baseResponse.setCode(BizErrorCodeEnum.SUCCESS.getCode());
        baseResponse.setMessage(BizErrorCodeEnum.SUCCESS.getDesc());

        PageInfo<ResponseBean> pageInfo = new PageInfo();
        if (!CollectionUtils.isEmpty(allList)) {
            int total = allList.size();
            Integer minIndex = (pageNum - 1) * pageSize;
            Integer maxIndex = pageNum * pageSize;

            pageInfo.setTotal(total);
            pageInfo.setPageNum(pageNum);
            pageInfo.setPageSize(pageSize);

            int pages = total / pageSize;
            if (total % pageSize != 0) {
                pages = pages + 1;
            }
            pageInfo.setPages(pages);

            List<ResponseBean> dataList = new ArrayList<>();
            if (total >= minIndex && total >= maxIndex) {
                dataList = allList.subList(minIndex, maxIndex);
            } else if (total >= minIndex && total < maxIndex) {
                dataList = allList.subList(minIndex, total);
            }
            if (pageNum > 1 && pageNum <= pages ) {
                pageInfo.setHasPreviousPage(true);
            } else {
                pageInfo.setHasPreviousPage(false);
            }
            if (pageNum < pages) {
                pageInfo.setHasNextPage(true);
            } else {
                pageInfo.setHasNextPage(false);
            }

            pageInfo.setList(dataList);
        }
        baseResponse.setData(pageInfo);
        return baseResponse;
    }

    /**
     * 将分页数据转换为DTO
     * @param pageInfoSource
     * @param targetClass
     * @param <Source>
     * @param <Target>
     * @return
     */
    private static <Source, Target> PageInfo<Target> toTarget(PageInfo<Source> pageInfoSource, Class<Target> targetClass) {
        PageInfo<Target> pageInfoTarget = new PageInfo<>();
        ConfigurableMapperFactory.getBeanMapper().map(pageInfoSource, pageInfoTarget);

        List<Target> targetList = ConfigurableMapperFactory.getBeanMapper().mapAsList(pageInfoSource.getList(), targetClass);
        pageInfoTarget.setList(targetList);

        return pageInfoTarget;
    }

    /**
     * 将分页数据转换为DTO
     *
     * @param sourceList
     * @param targetClass
     * @param <Source>
     * @param <Target>
     * @return
     */
    private static <Source, Target> PageInfo<Target> toTarget(List<Source> sourceList, Class<Target> targetClass) {
        // 处理分页数据
        PageInfo<Source> pageList = new PageInfo<>(sourceList);

        return toTarget(pageList, targetClass);
    }
}


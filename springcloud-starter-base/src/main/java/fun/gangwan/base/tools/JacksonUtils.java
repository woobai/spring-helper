package fun.gangwan.base.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import fun.gangwan.base.serialization.JacksonCustomizerConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;


/**
 *
 * <br>JacksonUtils</br>
 * <span>Jackson工具类</span>
 *
 * @author ZhouYi
 * @since 2022/03/15
 * @version 2.1.0
 *
 */
@Slf4j
@Configuration
@ConditionalOnClass(ObjectMapper.class)
@AutoConfigureAfter(JacksonCustomizerConfiguration.class)
public class JacksonUtils {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    ObjectMapper jacksonObjectMapper;

    @PostConstruct
    public void init(){
        if(jacksonObjectMapper == null){
            log.error("###### JacksonUtils init failed ######");
            return;
        }

        JacksonUtils.setInstance(jacksonObjectMapper);
        log.info("###### JacksonUtils init success ######");
    }

    /**
     * 设置实例
     * @param jacksonObjectMapper objMapper
     */
    public static void setInstance(ObjectMapper jacksonObjectMapper){
        JacksonUtils.objectMapper = jacksonObjectMapper;
    }

    /**
     * 获取Jackson实例
     * @return Jackson实例 可能为null
     */
    public static ObjectMapper getInstance(){
        return objectMapper;
    }

    /**
     * 判断字符串是否为合法的 JSON 对象
     *
     * @param json JSON 字符串
     * @return 是否为合法的 JSON 对象
     */
    public static boolean isValid(String json) {
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            return jsonNode.isObject();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为合法的 JSON 数组
     *
     * @param json JSON 字符串
     * @return 是否为合法的 JSON 数组
     */
    public static boolean isValidArray(String json) {
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            return jsonNode.isArray();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 格式化Json串
     * @param json json
     * @return prettyJson
     */
    public static String prettyJsonStr(String json) {
        if (StringUtils.isBlank(json)) {
            return json;
        }

        try {
            return obj2StringPretty(objectMapper.readValue(json, Object.class));
        } catch (JsonProcessingException e) {
            log.warn("obj2String error : {}", e.getMessage());
            return json;
        }
    }

    /**
     * 对象转Json格式字符串
     * @param obj 对象
     * @return Json格式字符串
     */
    public static <T> String obj2String(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return obj instanceof String ? (String) obj : objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Parse Object to String error : {}", e.getMessage());
            return null;
        }
    }

    /**
     * 对象转Json格式字符串(格式化的Json字符串)
     * @param obj 对象
     * @return 美化的Json格式字符串
     */
    public static <T> String obj2StringPretty(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return obj instanceof String ? (String) obj : objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Parse Object to String error : {}", e.getMessage());
            return null;
        }
    }

    /**
     * 序列化非空属性的对象
     * @param obj 对象
     * @param <T> 类型
     * @return 序列化结果
     */
    public static <T> String obj2StringNotNull(T obj){
        if(obj instanceof String){
            return (String) obj;
        }
        ObjectMapper disposableMapper = objectMapper.copy();
        //Value that indicates that only properties with non-null values are to be included.
        disposableMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return disposableMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Parse Object to String error : {}", e.getMessage());
            return null;
        }
    }

    /**
     * 序列化日期类型时以timestamps输出
     * @param obj 对象
     * @param <T> 类型
     * @return 序列化结果
     */
    public static <T> String obj2StringWithTimeStamps(T obj){
        ObjectMapper disposableMapper = objectMapper.copy();
        //序列化日期类型时以timestamps输出
        disposableMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        try {
            return disposableMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Parse Object to String error : {}", e.getMessage());
            return null;
        }
    }

    /**
     *
     * 字符串转换为自定义对象
     *
     * @param str 要转换的字符串
     * @param clazz 自定义对象的class对象
     * @return 自定义对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T string2Obj(String str, Class<T> clazz){
        if(StringUtils.isBlank(str) || clazz == null){
            return null;
        }
        try {
            return clazz.equals(String.class) ? (T) str : objectMapper.readValue(str, clazz);
        } catch (Exception e) {
            log.warn("Parse String to Object error : {}", e.getMessage());
            return null;
        }
    }

    /**
     *
     * json串转对象集合
     *
     * List<User> userListBean = JacksonUtils.string2ObjForRef(json, new TypeReference<List<User>>() {});
     *
     * @param str
     * @param typeReference
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T string2ObjForRef(String str, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(str) || typeReference == null) {
            return null;
        }
        try {
            return (T) (typeReference.getType().equals(String.class) ? str : objectMapper.readValue(str, typeReference));
        } catch (IOException e) {
            log.warn("Parse String to Object error", e);
            return null;
        }
    }

    /**
     *
     * json串转对象集合
     *
     * List<User> userListBean2 = JacksonUtils.string2ObjCollection(json, List.class, User.class);
     *
     * @param json
     * @param collectionClazz
     * @param elementClazzes
     * @param <T>
     * @return
     */
    public static <T> T string2ObjCollection(String json, Class<?> collectionClazz, Class<?>... elementClazzes) {
        if(StringUtils.isBlank(json) || collectionClazz == null || elementClazzes == null){
            return null;
        }
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(collectionClazz, elementClazzes);
        try {
            return objectMapper.readValue(json, javaType);
        } catch (IOException e) {
            log.warn("Jackson string2ObjCollection error : {}" + e.getMessage());
            return null;
        }
    }

    /**
     * String nodeJson = JacksonUtils.findNodeStrByKey(json, nodeKey);
     * @param json
     * @param nodeKey
     * @return
     */
    public static String findNodeStrByKey(String json, String nodeKey){
        if(StringUtils.isAnyBlank(json, nodeKey)){
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode keyNode = node.findValue(nodeKey);
            if(keyNode != null){
                return keyNode.toString();
            }
        } catch (JsonProcessingException e) {
            log.warn("Jackson findNodeStrByKey error : {}" + e.getMessage());
        }
        return null;
    }

    /**
     * User user = JacksonUtils.jsonNode2Obj(json, nodeKey, User.calss);
     * @param json
     * @param nodeKey
     * @param clazz
     * @return
     */
    public static <T> T jsonNode2Obj(String json, String nodeKey, Class<T> clazz){
        if(StringUtils.isBlank(json) || clazz == null){
            return null;
        }
        try {
            String nodeJson = findNodeStrByKey(json, nodeKey);
            return clazz.equals(String.class) ? (T) nodeJson : objectMapper.readValue(nodeJson, clazz);
        } catch (Exception e) {
            log.warn("Parse String to Object error : {}", e.getMessage());
            return null;
        }
    }

    /**
     * List<User> userListBean2 = JacksonUtils.jsonNode2ObjCollection(json, nodeKey, List.class, User.class);
     * @param json
     * @param nodeKey
     * @param collectionClazz
     * @param elementClazzes
     * @return
     */
    public static <T> T jsonNode2ObjCollection(String json, String nodeKey, Class<?> collectionClazz, Class<?>... elementClazzes){
        if(StringUtils.isAnyBlank(json, nodeKey) || collectionClazz == null || elementClazzes == null){
            return null;
        }
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(collectionClazz, elementClazzes);
        try {
            String node = findNodeStrByKey(json, nodeKey);
            return objectMapper.readValue(node, javaType);
        } catch (JsonProcessingException e) {
            log.warn("Jackson jsonNode2ObjCollection error : {}" + e.getMessage());
            return null;
        }
    }

    //********************************* JsonNode属性操作 **********************************//

    /**
     * 根据 JSON 字符串和属性路径获取属性值
     *
     * @param jsonString JSON 字符串
     * @param path       属性路径，例如："name.first"
     * @return 属性值
     * @throws IOException
     */
    public static JsonNode selectJsonNode(String jsonString, String path) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonString);
        String[] pathSegments = path.split("\\.");
        JsonNode node = rootNode;
        for (String pathSegment : pathSegments) {
            node = node.get(pathSegment);
            if (node == null) {
                break;
            }
        }
        return node;
    }

    /**
     * 根据 JSON 字符串和属性路径设置属性值
     *
     * @param jsonString JSON 字符串
     * @param path       属性路径，例如："name.first"
     * @param value      属性值
     * @return 更新后的 JSON 字符串
     * @throws IOException
     */
    public static String updatePropertyValue(String jsonString, String path, String value) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonString);
        String[] pathSegments = path.split("\\.");
        ObjectNode parentNode = (ObjectNode) rootNode;
        for (int i = 0; i < pathSegments.length - 1; i++) {
            String pathSegment = pathSegments[i];
            JsonNode childNode = parentNode.get(pathSegment);
            if (childNode == null || !childNode.isObject()) {
                childNode = objectMapper.createObjectNode();
                parentNode.set(pathSegment, childNode);
            }
            parentNode = (ObjectNode) childNode;
        }
        String lastPathSegment = pathSegments[pathSegments.length - 1];
        parentNode.set(lastPathSegment, new TextNode(value));
        return objectMapper.writeValueAsString(rootNode);
    }

    /**
     * 根据 JSON 字符串和属性路径删除属性
     *
     * @param jsonString JSON 字符串
     * @param path       属性路径，例如："name.first"
     * @return 更新后的 JSON 字符串
     * @throws IOException
     */
    public static String deleteJsonValue(String jsonString, String path) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonString);
        String[] pathSegments = path.split("\\.");
        ObjectNode parentNode = (ObjectNode) rootNode;
        for (int i = 0; i < pathSegments.length - 1; i++) {
            String pathSegment = pathSegments[i];
            JsonNode childNode = parentNode.get(pathSegment);
            if (childNode == null || !childNode.isObject()) {
                childNode = objectMapper.createObjectNode();
                parentNode.set(pathSegment, childNode);
            }
            parentNode = (ObjectNode) childNode;
        }
        String lastPathSegment = pathSegments[pathSegments.length - 1];
        parentNode.remove(lastPathSegment);
        return objectMapper.writeValueAsString(rootNode);
    }

    /**
     * 在 JSON 数组的指定位置插入指定元素
     * JacksonUtils.insertJsonProperty(jsonString, "items", 1, value);
     *
     * @param jsonString JSON 字符串
     * @param path       数组属性路径，例如："items"
     * @param index      插入的位置
     * @param propertyK      插入的元素
     * @param propertyV      插入的元素
     * @return 更新后的 JSON 字符串
     * @throws IOException
     */
    public static String insertJsonProperty(String jsonString, String path, int index, String propertyK, String propertyV)
            throws IOException {
        JsonNode value = objectMapper.createObjectNode().put(propertyK, propertyV);
        JsonNode rootNode = objectMapper.readTree(jsonString);
        String[] pathSegments = path.split("\\.");
        ObjectNode parentNode = (ObjectNode) rootNode;
        for (int i = 0; i < pathSegments.length - 1; i++) {
            String pathSegment = pathSegments[i];
            JsonNode childNode = parentNode.get(pathSegment);
            if (childNode == null || !childNode.isObject()) {
                childNode = objectMapper.createObjectNode();
                parentNode.set(pathSegment, childNode);
            }
            parentNode = (ObjectNode) childNode;
        }
        String lastPathSegment = pathSegments[pathSegments.length - 1];
        JsonNode arrayNode = parentNode.get(lastPathSegment);
        if (arrayNode == null || !arrayNode.isArray()) {
            arrayNode = objectMapper.createArrayNode();
            parentNode.set(lastPathSegment, arrayNode);
        }
        ArrayNode newArrayNode = objectMapper.createArrayNode();
        for (int i = 0; i < arrayNode.size(); i++) {
            if (i == index) {
                newArrayNode.add(value);
            }
            newArrayNode.add(arrayNode.get(i));
        }
        if (index == arrayNode.size()) {
            newArrayNode.add(value);
        }
        parentNode.set(lastPathSegment, newArrayNode);
        return objectMapper.writeValueAsString(rootNode);
    }

    /**
     * jsonNode的安全get方法，避免空指针
     *
     * @param jsonNode  jsonNode对象
     * @param fieldName 字段名称
     * @param getValue  字段值的get方法
     * @return 字段值，可能为空
     */
    @Nullable
    public static <T> T jsonNodeGet(JsonNode jsonNode, String fieldName, Function<JsonNode, T> getValue) {
        return Optional.ofNullable(jsonNode)
                .map(json -> json.get(fieldName))
                .map(getValue)
                .orElse(null);
    }
}
package ru.cfttask.cfttask;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.everit.json.schema.*;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.*;

import ru.cfttask.cfttask.Response;
import com.google.gson.Gson;

import org.apache.http.Header;

public class HttpRequestTest {

	public static void main(String[] args) {
		
		System.out.println("Test N1 - Запрос с параметрами");
		String url = "https://newsapi.org/v2/top-headlines";
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("sources", "time"));
		params.add(new BasicNameValuePair("apiKey", "it_is_not_a_key"));
		
		httpResponseTest(url, params);
		
		System.out.println("\n--------------------------------------");
		System.out.println("Test N2 - запрос без параметров");
		List<NameValuePair> params2 = new ArrayList<>();
		
		httpResponseTest(url, params2);
        		
}
	// Функция, в которой выполняются все проверки из задания
	public static void httpResponseTest(String url, List<NameValuePair> params) {
		
		String uri = createURI(url, params);									// создаем uri из урла и параметров
		System.out.println("URI запроса:\n" + uri);
		
		String httpResp = sendRequest(uri, false);								// ответ в виде строки. 2-ой аргумент запрещает выводить заголовки в консоль
		System.out.println("Ответ:\n" + httpResp);

/*
 * 1. Провалидировать, используя JSONSchema       
 */		
		System.out.println("\nВалидаций корректной схемой good_schema.json");
		String validSchema = new String("./good_schema.json");					 
        validateJson(validSchema, httpResp);									// валидация корректной Json-схемой

        System.out.println("\nВалидаций некорректной схемой bad_schema.json");
        String invalidSchema = new String("./bad_schema.json");
        validateJson(invalidSchema, httpResp);									// валидация некорректной Json-схемой: у поля status тип integer
        
/*
 * 2. Проверить, что значение поля "status" = "error"        
 */
        System.out.println();
        checkValueOfField(httpResp, "status", "error");							// проверка значений различных полей
        checkValueOfField(httpResp, "code", "apiKeyInvalid");
        checkValueOfField(httpResp, "code", "123");
        
/*
 * 3. Десериализовать полученный json в модель. Проверить, что в строке message присутствует подстрока "free API key"        
 */
        
        System.out.println();
        String searchStr = "free API key";
        String nameField = "message"; 
        
		checkFieldWhenSerial(httpResp, searchStr, nameField);					// проверки на наличие подстроки
		checkFieldWhenSerial(httpResp, "free API KEYS", "message");
	}
	
	// Функция проверки наличия подстроки через десериализацию
	public static void checkFieldWhenSerial(String httpResp, String searchStr, String nameField) 
	{
		Gson gson = new Gson();
        Response objResp = new Response();
        objResp = gson.fromJson(httpResp, Response.class);										// Десериализуем ответ в объект класса Response
        objResp.printResponse();

        Field field;																			// Поле
        String value = new String();															// Значение поля
		try {
			field = objResp.getClass().getDeclaredField(nameField);								// Получаем ссылку на искомое поле
			value = field.get(objResp).toString();												// Получаем значение искомого поля
		} catch (NoSuchFieldException | SecurityException e) {
			System.out.println("Error: ошибка доступа к полю или такого поля не существует");
			e.printStackTrace();
		} catch (IllegalArgumentException | IllegalAccessException e) {
			System.out.println("Error: несуществующий аргумент");
			e.printStackTrace();
		}											

        if (value.contains(searchStr)) {														// Проверяем, содержится ли искомая подстрока в заданном поле
        	System.out.println("В поле " + nameField + " содержится строка \'" + searchStr + "\'");
        } 
        else {
        	System.out.println("В поле " + nameField + " не содержится строка \'" + searchStr + "\'");
        }
	}

	// Функция проверки значения поля через json
	public static void checkValueOfField(String json, String field, String value) {
		JSONObject jsonObj = new JSONObject(json);												// Формируем JSONObject из строкового представления json
		String fieldValue = jsonObj.getString(field);											// Получаем значение искомого поля
		
		if (fieldValue.contains(value)) {
			System.out.println("Поле " + field + " = " + value);
		} else {
			System.out.println("Поле " + field + " <> " + value);
		}
	}

	// Валидация с помощью Json-схемы
	public static void validateJson(String pathToSchema, String json)  {
		try (InputStream inputStream = HttpRequestTest.class.
				getClassLoader().getResourceAsStream(pathToSchema)) {
		
			JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));				// Формируем JSONObject из json-схемы
			
			Schema schema = SchemaLoader.load(rawSchema);
			schema.validate(new JSONObject(json)); 												// Выполняем валидацию
			System.out.println("Валидация завершена успешно");
		} catch (IOException e) {
			System.out.println("Error: проблемы при закрытии потока InputStream");
			e.printStackTrace();
		} catch (ValidationException e) {														// если json невалидный, выбрасывается ValidationException
			System.out.println("Error: ошибка при выполнении валидации");
//			e.printStackTrace();
		}
		
	}

	// Формирование URI
	public static String createURI(String url, List<NameValuePair> params) {
		String uri = new String();
		
		try {
			URIBuilder builder = new URIBuilder(url).addParameters(params);						// Добавляем параметры запроса
			uri = builder.build().toString();													// Формируем uri в виде строки
		} catch (URISyntaxException e) {
			System.out.println("Error: Ошибка при формировании URI");
			e.printStackTrace();
		}
		return uri;
	}

	// Отправка запроса
	public static String sendRequest(String uri, boolean displayHeaders) {
		String response = new String();

		try (
				CloseableHttpClient client = HttpClients.createDefault();
				CloseableHttpResponse httpResp = client.execute(new HttpGet(uri))				// Выполняем get-запрос
        ) {
			HttpEntity entity = httpResp.getEntity();
            if (entity != null) {
                response = IOUtils.toString(entity.getContent());								// Формируем ответ
            }
            
            if (displayHeaders) {																// Выводим заголовки ответа
	            System.out.println();
	            for (Header header: httpResp.getAllHeaders()) {
	                System.out.println(header.getName() + " : " + header.getValue());
	            }
	            System.out.println(httpResp.getStatusLine());
	            System.out.println("Protocol version: " + httpResp.getProtocolVersion());
	            System.out.println("Status code: " + httpResp.getStatusLine().getStatusCode());
	            System.out.println("Reason phrase: " + httpResp.getStatusLine().getReasonPhrase());
	            System.out.println("Status line: " + httpResp.getStatusLine().toString());
            }
            
		} catch (IOException e) {
			System.out.println("Erorr: Ошибка при выполнении Get-запроса");
			e.printStackTrace();
		}
		
		return response;
	}
}

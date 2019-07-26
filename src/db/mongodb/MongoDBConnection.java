package db.mongodb;

import java.util.List;
import java.util.Set;

import java.util.HashSet;
import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.eq;

import entity.Item.ItemBuilder;
import external.TicketMasterAPI;
import db.DBConnection;
import entity.Item;

public class MongoDBConnection implements DBConnection {
	private MongoClient mongoClient;
	private MongoDatabase db;
	
	public MongoDBConnection() {
		mongoClient = new MongoClient();
		db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);
	}
	@Override
	public void close() {
		// TODO Auto-generated method stub
		if (mongoClient != null) {
			mongoClient.close();
		}
	}

	@Override
	public void setFavoriteItems(String userId, String itemIds) { // ���favorite�����ղأ����ݿ⣩
		// TODO Auto-generated method stub
		// һ��key-value pair��һ��new Document
		db.getCollection("users").updateOne(new Document("user_id", userId),
//				new Document("$push", new Document("favorite", new Document("$each", itemIds)))); // һ��һ������favorite
				new Document("$push", new Document("favorite", itemIds))); // String? List<String>
	}

	@Override
	public void unsetFavoriteItems(String userId, String itemIds) {
		// TODO Auto-generated method stub
		db.getCollection("users").updateOne(new Document("user_id", userId),
				new Document("$pullAll", new Document("favorite", itemIds))); // �Ƴ�����favorite��value
	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		// TODO Auto-generated method stub
		Set<String> favoriteItems = new HashSet<>();
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		
		if (iterable.first() != null && iterable.first().containsKey("favorite")) { // ���ǿ��Ҵ���favorite��key
			@SuppressWarnings("unchecked") // ����ǿ��ת��û������
			List<String> list = (List<String>) iterable.first().get("favorite");
			favoriteItems.addAll(list);
		}

		return favoriteItems;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		// TODO Auto-generated method stub
		Set<Item> favoriteItems = new HashSet<>();
		
		Set<String> itemIds = getFavoriteItemIds(userId); // �ҵ���Ӧuser��favorite��item��ids
		for (String itemId : itemIds) { // ����itemid��item��Ϣ
			FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", itemId));
			if (iterable.first() != null) {
				Document doc = iterable.first(); // �ҵ���Ӧ��document
				
				ItemBuilder builder = new ItemBuilder(); // ʹ��builder����������̫��ʱ�ɱ�������constructor
				builder.setItemId(doc.getString("item_id"));
				builder.setName(doc.getString("name"));
				builder.setAddress(doc.getString("address"));
				builder.setUrl(doc.getString("url"));
				builder.setImageUrl(doc.getString("image_url"));
				builder.setRating(doc.getDouble("rating"));
				builder.setDistance(doc.getDouble("distance"));
				builder.setCategories(getCategories(itemId));
				
				favoriteItems.add(builder.build()); // ������item
			}
		}

		return favoriteItems;

	}

	@Override
	public Set<String> getCategories(String itemId) {
		// TODO Auto-generated method stub
		Set<String> categories = new HashSet<>();
		FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", itemId));
		
		if (iterable.first() != null && iterable.first().containsKey("categories")) { // ����categories���field�������
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>) iterable.first().get("categories");
			categories.addAll(list);
		}

		return categories;

	}

	@Override
	public List<Item> searchItems(double lat, double lon, String term) {
		// TODO Auto-generated method stub // ��TicketMaster������
		TicketMasterAPI tmAPI = new TicketMasterAPI();
		List<Item> items = tmAPI.search(lat, lon, term);
		for (Item item : items) {
			saveItem(item); // �Զ��庯����
		}
		return items; 
	}

	@Override
	public void saveItem(Item item) {
		// TODO Auto-generated method stub // �������д�룬 û��ignore
		FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", item.getItemId())); // ����
		// ���Դ��������� IGNORE
		if (iterable.first() == null) {
			db.getCollection("items")
					.insertOne(new Document().append("item_id", item.getItemId()).append("distance", item.getDistance())
							.append("name", item.getName()).append("address", item.getAddress())
							.append("url", item.getUrl()).append("image_url", item.getImageUrl())
							.append("rating", item.getRating()).append("categories", item.getCategories()));
		}

		
	}

	@Override
	public String getFullname(String userId) {
		// TODO Auto-generated method stub
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		if (iterable.first() != null) {
			Document doc = iterable.first();
			return doc.getString("first_name") + " " + doc.getString("last_name");
		}
		return "";
	}

	@Override
	public boolean verifyLogin(String userId, String password) {
		// TODO Auto-generated method stub
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		if (iterable.first() != null) {
			Document doc = iterable.first();
			boolean res = doc.getString("password").equals(password);
			if (res == true) {
				System.out.print("succeed");
				return true;
			} else {
				System.out.print("password does not match");
				return false;
			}
		}
		System.out.print("user does not exist");
		return false;
	}

	@Override
	public boolean registerUser(String userId, String password, String firstname, String lastname) {
		// TODO Auto-generated method stub
		if (mongoClient == null) {
			System.err.println("DB connection failed");
			return false;
		}
		//implement register
		try {
			FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
			if (iterable.first() == null) {
				db.getCollection("users").insertOne(new Document().append("user_id", userId).append("password", password).append("first_name", firstname).append("last_name", lastname));
				System.out.print("registered");
				return true;
			} else {
				System.out.print("user already exist");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	
		return true;
	}

}

package db.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.TicketMasterAPI;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQLConnection implements DBConnection {// implement interface
	private Connection conn;
	   
    public MySQLConnection() {
  	  try {
  		  Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance(); // jvm����com.mysql.cj.jdbc.Driver�࣬jdbc��������mysql���ݿ�
  		  conn = DriverManager.getConnection(MySQLDBUtil.URL);
  		
  	  } catch (Exception e) {
  		  e.printStackTrace();
  	  }
    }

	@Override
	public void close() {
		// TODO Auto-generated method stub
		if(conn != null) {
			try {
				conn.close(); // close connection
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@Override
	public void setFavoriteItems(String userId, String itemId) {
		// TODO Auto-generated method stub
		
		 if (conn == null) {
	  		 System.err.println("DB connection failed");
	  		 return;
	  	 }
	  	
	  	 try {
	  		 String sql = "INSERT IGNORE INTO history(user_id, item_id) VALUES (?, ?)";
	  		 PreparedStatement ps = conn.prepareStatement(sql); // ׼����� 
	  		 ps.setString(1, userId);
	         ps.setString(2, itemId);
	         ps.execute(); // ִ�в������
	  	 } catch (Exception e) {
	  	     e.printStackTrace();
	       }


	}

	@Override
	public void unsetFavoriteItems(String userId, String itemId) {
		// TODO Auto-generated method stub
        if (conn == null) {
			System.err.println("DB connection failed");
			return;
        }
			
		try {
			String sql = "DELETE FROM history WHERE user_id = ? AND item_id = ?"; // ȷ����һ������
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, userId);
			ps.setString(2, itemId);
			ps.execute();
		
		
		 } catch (Exception e) {
			 e.printStackTrace();
		 }

	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) { // ����set���ظ�
		// TODO Auto-generated method stub
		if (conn == null) {
			return new HashSet<>();
		}
		
		Set<String> favoriteItems = new HashSet<>();
		
		try {
			String sql = "SELECT item_id FROM history WHERE user_id = ?"; // id��ͬ��column
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, userId);
			
			ResultSet rs = stmt.executeQuery(); // read from history table to get favorite items' ids
			
			while (rs.next()) {
				String itemId = rs.getString("item_id");
				favoriteItems.add(itemId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return favoriteItems;

	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		// TODO Auto-generated method stub
		if (conn == null) {
			return new HashSet<>();
		}
		
		Set<Item> favoriteItems = new HashSet<>();
		Set<String> itemIds = getFavoriteItemIds(userId); //��history table�������õ����û�ϲ������Ʒ��id
		
		try {
			String sql = "SELECT * FROM items WHERE item_id = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			for (String itemId : itemIds) { // get favs by ids
				stmt.setString(1, itemId);
				
				ResultSet rs = stmt.executeQuery(); // ��item table��ȡ����
				
				ItemBuilder builder = new ItemBuilder(); // use item builder to build items
				
				while (rs.next()) {
					builder.setItemId(rs.getString("item_id"));
					builder.setName(rs.getString("name"));
					builder.setAddress(rs.getString("address"));
					builder.setImageUrl(rs.getString("image_url"));
					builder.setUrl(rs.getString("url"));
					builder.setCategories(getCategories(itemId)); // ��category list�л�ȡ����
					builder.setDistance(rs.getDouble("distance"));
					builder.setRating(rs.getDouble("rating"));
					
					favoriteItems.add(builder.build()); // build items with builder
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return favoriteItems;
	}

	@Override
	public Set<String> getCategories(String itemId) {
		// TODO Auto-generated method stub
		if (conn == null) {
			return null;
		}
		Set<String> categories = new HashSet<>();
		try {
			String sql = "SELECT category from categories WHERE item_id = ? "; // ��category�ж�ȡ
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, itemId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				String category = rs.getString("category");
				categories.add(category);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return categories;

	}

	@Override
	public List<Item> searchItems(double lat, double lon, String term) {
		// TODO Auto-generated method stub
		TicketMasterAPI ticketMasterAPI = new TicketMasterAPI();
	    List<Item> items = ticketMasterAPI.search(lat, lon, term);
	
	    for(Item item : items) {
	    	saveItem(item);
	    }
	
	    return items;
	}

	@Override
	public void saveItem(Item item) {
		// TODO Auto-generated method stub, item table
		if (conn == null) {
			System.err.println("DB connection failed");
	  		return;
		}
		
	  	 try {
	  		 String sql = "INSERT IGNORE INTO items VALUES (?, ?, ?, ?, ?, ?, ?)"; // ����ÿһ��column�� ��ռλ��, save to items table
	  		 PreparedStatement ps = conn.prepareStatement(sql); // ׼�����
	  		 ps.setString(1, item.getItemId()); // ����д�����
	  		 ps.setString(2, item.getName());
	  		 ps.setDouble(3, item.getRating());
	  		 ps.setString(4, item.getAddress());
	  		 ps.setString(5, item.getImageUrl());
	  		 ps.setString(6, item.getUrl());
	  		 ps.setDouble(7, item.getDistance());
	  		 ps.execute(); // ִ�����
	  		
	  		 sql = "INSERT IGNORE INTO categories VALUES(?, ?)"; // save to categories table
	  		 ps = conn.prepareStatement(sql); // ׼�����
	  		 ps.setString(1, item.getItemId()); // �������ֵ
	  		 for(String category : item.getCategories()) {
	  			 ps.setString(2, category); // category list: item id, category.
	  			 ps.execute();
	  		 }
	  		
	  	 } catch (Exception e) {
	  		 e.printStackTrace();
	  	 }
	}

	@Override
	public String getFullname(String userId) { // ��id���û�ȫ��
		// TODO Auto-generated method stub
		if (conn == null) {
			return ""; // sanity check
		}		
		String name = "";
		try {
			String sql = "SELECT first_name, last_name FROM users WHERE user_id = ? "; // ��ȡ=select������where����
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId); // ���
			ResultSet rs = statement.executeQuery(); // ִ��
			while (rs.next()) {
				name = rs.getString("first_name") + " " + rs.getString("last_name"); // ��ȡ����
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return name;

	}

	@Override
	public boolean verifyLogin(String userId, String password) { // ������֤user id, password����
		// TODO Auto-generated method stub
		if (conn == null) {
			return false;
		}
		try {
			String sql = "SELECT user_id FROM users WHERE user_id = ? AND password = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, userId);
			stmt.setString(2, password);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return true; // ��֤��select��id
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return false;

	}

	@Override
	public boolean registerUser(String userId, String password, String firstname, String lastname) {
		// TODO Auto-generated method stub
		if (conn == null) {
			System.err.println("DB connection failed");
			return false;
		}

		try {
			String sql = "INSERT IGNORE INTO users VALUES (?, ?, ?, ?)"; // ����ֹsqlע�룬��ֹ��ͨ�����빥��
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, userId);
			ps.setString(2, password);
			ps.setString(3, firstname);
			ps.setString(4, lastname);
			
			return ps.executeUpdate() == 1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;	
	}

}

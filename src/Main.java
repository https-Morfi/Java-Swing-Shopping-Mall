import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main {

    public static final String PRODUCTS_CSV = "products.csv";
    public static final String CART_CSV = "cart.csv";

    public static void main(String[] args) {

        ProductService productService = new ProductService();
        ProductFileRepository repository = new ProductFileRepository(PRODUCTS_CSV);
        CartFileRepository cartRepository = new CartFileRepository(CART_CSV);


        productService.loadFromFile(repository);


        MainFrame frame = new MainFrame();

        LoginPanel loginPanel = new LoginPanel(frame);
        AdminPanel adminPanel = new AdminPanel(productService, repository);
        CustomerPanel customerPanel = new CustomerPanel(productService, repository, cartRepository);

        frame.addPanel(loginPanel, MainFrame.LOGIN);
        frame.addPanel(adminPanel, MainFrame.ADMIN);
        frame.addPanel(customerPanel, MainFrame.CUSTOMER);

        frame.showPanel(MainFrame.LOGIN);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {

                productService.saveToFile(repository);
                cartRepository.saveCart(customerPanel.getShoppingCart());

                System.exit(0);
            }
        });

        frame.setVisible(true);

    }
}
enum Category {
    ELECTRONICS,
    CLOTHING,
    FOOD,
    BOOKS,
    GENERAL
}

class Product {
    private String id;
    private String name;
    private Category category;
    private double price;
    private int stock;
    private String description;
    private String imagePath;

    public Product(String name, Category category, double price, int stock, String description) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.description = description;
    }

    // Constructor dovom baraye vaghti Product az file miad va id avaz shode
    public Product(String id, String name, Category category, double price, int stock, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.description = description;
    }

    public void increaseStock(int amount) {
        stock += amount;
    }

    public boolean decreaseStock(int amount) {
        if (amount > stock) {
            return false;
        }
        stock -= amount;
        return true;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public String getDescription() {
        return description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setName(String name) { this.name = name; }

    public void setCategory(Category category) { this.category = category; }

    public void setPrice(double price) { this.price = price; }

    public void setStock(int stock) { this.stock = stock; }

    public void setDescription(String description) { this.description = description; }
}

class CartItem {

    private Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void increaseQuantity(int amount) {
        if (amount > 0) {
            quantity += amount;
        }
    }

    public void decreaseQuantity(int amount) {
        if (amount > 0) {
            quantity -= amount;
            if (quantity < 0) {
                quantity = 0;
            }
        }
    }

    public double getTotalPrice() {
        return product.getPrice() * quantity;
    }
}

class ShoppingCart {

    private List<CartItem> items;

    public ShoppingCart() {
        items = new ArrayList<>();
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void addProduct(Product product, int quantity) {
        if (quantity <= 0) {
            return;
        }

        for (CartItem item : items) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.increaseQuantity(quantity);
                return;
            }
        }

        items.add(new CartItem(product, quantity));
    }

    public void removeProduct(Product product) {
        for (int i = 0; i < items.size(); i++) {
            CartItem item = items.get(i);
            if (item.getProduct().getId().equals(product.getId())) {
                items.remove(i);
                break;
            }
        }
    }

    public void decreaseProduct(Product product, int quantity) {
        if (quantity <= 0) {
            return;
        }

        for (CartItem item : items) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.decreaseQuantity(quantity);
                if (item.getQuantity() == 0) {
                    removeProduct(product);
                }
                return;
            }
        }
    }

    public double getTotalPrice() {
        double total = 0;
        for (CartItem item : items) {
            total += item.getTotalPrice();
        }
        return total;
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}

abstract class User {

    protected String username;
    protected String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public boolean checkPassword(String input) {
        return password.equals(input);
    }

    public abstract String getRole();
}

class Customer extends User {

    private ShoppingCart cart;

    public Customer(String username, String password) {
        super(username, password);
        this.cart = new ShoppingCart();
    }

    public ShoppingCart getCart() {
        return cart;
    }

    public String getRole() {
        return "CUSTOMER";
    }
}

class Administrator extends User {

    public Administrator(String username, String password) {
        super(username, password);
    }

    public String getRole() {
        return "ADMIN";
    }
}

class ProductService {

    private List<Product> products;

    public ProductService() {
        products = new ArrayList<>();
    }

    public void addProduct(Product product) {
        products.add(product);
    }

    public void removeProductById(String productId) {
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).getId().equals(productId)) {
                products.remove(i);
                break;
            }
        }
    }

    public Product findById(String productId) {
        for (Product product : products) {
            if (product.getId().equals(productId)) {
                return product;
            }
        }
        return null;
    }

    public List<Product> getAllProducts() {
        return products;
    }

    public void loadFromFile(ProductFileRepository repository) {
        products = repository.loadProducts();
    }

    public void saveToFile(ProductFileRepository repository) {
        repository.saveProducts(products);
    }

    public void setProductImage(String productId, String imagePath) {
        Product product = findById(productId);
        if (product != null) {
            product.setImagePath(imagePath);
        }
    }
}

class CartService {

    private ShoppingCart cart;
    private ProductService productService;

    public CartService(ShoppingCart cart, ProductService productService) {
        this.cart = cart;
        this.productService = productService;
    }

    public void addToCart(Product product, int quantity) {
        if (product.getStock() >= quantity) {
            cart.addProduct(product, quantity);
        }
    }

    public void removeFromCart(Product product) {
        cart.removeProduct(product);
    }

    public double getTotalPrice() {
        return cart.getTotalPrice();
    }

    public boolean checkout(ProductFileRepository repository) {
        if (cart.isEmpty()) {
            return false;
        }

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            int quantity = item.getQuantity();

            if (product.getStock() < quantity) {
                return false;
            }
        }

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            product.decreaseStock(item.getQuantity());
        }

        cart.clear();

        productService.saveToFile(repository);

        return true;
    }
}

class ProductFileRepository {

    private final String filePath;

    public ProductFileRepository(String filePath) {
        this.filePath = filePath;
    }

    public void saveProducts(List<Product> products) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {

            for (Product product : products) {

                String imagePath = product.getImagePath();
                if (imagePath == null) {
                    imagePath = "";
                }

                String line = product.getId() + ";" + product.getName() + ";" + product.getCategory() + ";" +
                        product.getPrice() + ";" + product.getStock() + ";" + product.getDescription() + ";" + imagePath;

                writer.println(line);
            }

        } catch (IOException e) {
            System.out.println("Error saving products to file.");
        }
    }

    public List<Product> loadProducts() {
        List<Product> products = new ArrayList<>();

        File file = new File(filePath);
        if (!file.exists()) {
            return products;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;
            while ((line = reader.readLine()) != null) {

                String[] parts = line.split(";");

                if (parts.length != 7) {
                    continue;
                }

                String id = parts[0];
                String name = parts[1];
                Category category = Category.valueOf(parts[2]);
                double price = Double.parseDouble(parts[3]);
                int stock = Integer.parseInt(parts[4]);
                String description = parts[5];
                String imagepath = parts[6];
                if (imagepath != null && imagepath.trim().isEmpty()) {
                    imagepath = null;
                }

                Product product = new Product(id, name, category, price, stock, description);
                product.setImagePath(imagepath);

                products.add(product);
            }

        } catch (IOException e) {
            System.out.println("Error loading products from file.");
        }

        return products;
    }
}

class CartFileRepository {

    private final String filePath;

    public CartFileRepository(String filePath) {
        this.filePath = filePath;
    }

    public void saveCart(ShoppingCart cart) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            for (CartItem item : cart.getItems()) {
                String line = item.getProduct().getId() + ";" + item.getQuantity();
                writer.println(line);
            }
        } catch (IOException e) {
            System.out.println("Error saving cart to file: " + e.getMessage());
        }
    }

    public ShoppingCart loadCart(ProductService productService) {
        ShoppingCart cart = new ShoppingCart();

        File file = new File(filePath);
        if (!file.exists()) {
            return cart;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length != 2) continue;

                String productId = parts[0].trim();
                int qty;
                try {
                    qty = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ex) {
                    continue;
                }

                Product p = productService.findById(productId);
                if (p == null) {
                    continue;
                }

                if (p.getStock() < qty) {
                    qty = p.getStock();
                }

                if (qty > 0) {
                    cart.addProduct(p, qty);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading cart from file: " + e.getMessage());
        }

        return cart;
    }
}

class MainFrame extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;

    public static final String LOGIN = "LOGIN";
    public static final String ADMIN = "ADMIN";
    public static final String CUSTOMER = "CUSTOMER";

    public MainFrame() {
        setTitle("Shopping Mall");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        add(mainPanel);
    }

    public void addPanel(JPanel panel, String name) {
        mainPanel.add(panel, name);
    }

    public void showPanel(String name) {
        cardLayout.show(mainPanel, name);
    }
}

class AuthService {

    public enum Role {
        ADMIN,
        CUSTOMER,
        NONE
    }

    public Role login(String username, String password) {

        if ("admin".equals(username) && "admin4321".equals(password)) {
            return Role.ADMIN;
        }

        if ("customer".equals(username) && "1234".equals(password)) {
            return Role.CUSTOMER;
        }

        return Role.NONE;
    }
}

class LoginPanel extends JPanel {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;

    private AuthService authService;
    private MainFrame mainFrame;

    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.authService = new AuthService();

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Shopping Mall Login");
        title.setFont(new Font("Arial", Font.BOLD, 20));

        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        loginButton = new JButton("Login");

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        add(loginButton, gbc);

        loginButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        AuthService.Role role = authService.login(username, password);

        if (role == AuthService.Role.ADMIN) {
            mainFrame.showPanel(MainFrame.ADMIN);
        }
        else if (role == AuthService.Role.CUSTOMER) {
            mainFrame.showPanel(MainFrame.CUSTOMER);
        }
        else {
            JOptionPane.showMessageDialog(
                    this,
                    "Invalid username or password",
                    "Login Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}

class AdminPanel extends JPanel {

    private ProductService productService;
    private ProductFileRepository repository;

    private JTable table;
    private DefaultTableModel tableModel;

    private JButton addButton;
    private JButton deleteButton;
    private JButton addImageButton;
    private JButton editButton;

    public AdminPanel(ProductService productService, ProductFileRepository repository) {
        this.productService = productService;
        this.repository = repository;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initTable();
        initButtons();

        refreshTable();
    }

    private void initTable() {
        String[] columns = { "ID", "Name", "Category", "Price", "Stock", "Image"};

        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void initButtons() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        addButton = new JButton("Add Product");
        deleteButton = new JButton("Delete Product");
        addImageButton = new JButton("Add / Change Image");
        editButton = new JButton("Edit Product");

        buttonPanel.add(editButton);
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(addImageButton);

        add(buttonPanel, BorderLayout.NORTH);

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addProduct();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteProduct();
            }
        });

        addImageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooseImage();
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { editProduct(); }
        });
    }

    private void addProduct() {
        String name = JOptionPane.showInputDialog(this, "Product name:");
        if (name == null || name.isEmpty()) return;

        String category = JOptionPane.showInputDialog(this, "Category:");
        if (category == null) return;

        Category categoryEnum;
        try {
            categoryEnum = Category.valueOf(category.trim().toUpperCase());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid category");
            return;
        }

        String priceStr = JOptionPane.showInputDialog(this, "Price:");
        String stockStr = JOptionPane.showInputDialog(this, "Stock:");

        double price;
        int stock;
        try {
            price = Double.parseDouble(priceStr);
            stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Price or stock must be a number");
            return;
        }

        Product product = new Product(name, categoryEnum , price, stock, "");
        productService.addProduct(product);

        productService.saveToFile(repository);

        refreshTable();
    }

    private void deleteProduct() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a product first");
            return;
        }

        String id = (String) tableModel.getValueAt(selectedRow, 0);
        productService.removeProductById(id);

        productService.saveToFile(repository);

        refreshTable();
    }

    private void chooseImage() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a product first");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File chosenFile = chooser.getSelectedFile();

            String relativePath = copyFileToImages(chosenFile);

            if (relativePath == null) {
                JOptionPane.showMessageDialog(this, "Failed to copy image to images/ folder.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String id = (String) tableModel.getValueAt(selectedRow, 0);

            productService.setProductImage(id, relativePath);

            productService.saveToFile(repository);

            refreshTable();
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);

        for (Product p : productService.getAllProducts()) {
            tableModel.addRow(new Object[]{
                    p.getId(),
                    p.getName(),
                    p.getCategory(),
                    p.getPrice(),
                    p.getStock(),
                    p.getImagePath()
            });
        }
    }

    private String copyFileToImages(File sourceFile) {
        if (sourceFile == null) return null;

        String imagesDirName = "images";
        File imagesDir = new File(imagesDirName);

        if (!imagesDir.exists()) {
            boolean ok = imagesDir.mkdirs();
            if (!ok) {
                return null;
            }
        }

        String originalName = sourceFile.getName();
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalName.substring(dot);
        }

        String uniqueName = UUID.randomUUID().toString() + ext;

        Path destPath = Paths.get(imagesDirName, uniqueName);

        try {
            Files.copy(sourceFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
            return imagesDirName + "/" + uniqueName;

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void editProduct() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a product first");
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        String id = (String) tableModel.getValueAt(modelRow, 0);

        Product product = productService.findById(id);
        if (product == null) {
            JOptionPane.showMessageDialog(this, "Product not found");
            return;
        }

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;


        form.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(product.getName(), 20);
        form.add(nameField, gbc);


        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        JComboBox<Category> categoryBox = new JComboBox<>(Category.values());
        categoryBox.setSelectedItem(product.getCategory());
        form.add(categoryBox, gbc);


        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Price:"), gbc);
        gbc.gridx = 1;
        JTextField priceField = new JTextField(String.valueOf(product.getPrice()), 10);
        form.add(priceField, gbc);


        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Stock:"), gbc);
        gbc.gridx = 1;
        JTextField stockField = new JTextField(String.valueOf(product.getStock()), 10);
        form.add(stockField, gbc);


        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        JTextArea descArea = new JTextArea(product.getDescription(), 4, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        form.add(descScroll, gbc);


        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Image:"), gbc);
        gbc.gridx = 1;
        JPanel imgPanel = new JPanel(new BorderLayout(6,6));
        JLabel imgLabel = new JLabel(product.getImagePath() == null ? "No image" : product.getImagePath());
        JButton changeImgBtn = new JButton("Change Image");
        imgPanel.add(imgLabel, BorderLayout.CENTER);
        imgPanel.add(changeImgBtn, BorderLayout.EAST);
        form.add(imgPanel, gbc);


        final String[] newImagePath = new String[1];
        newImagePath[0] = product.getImagePath();


        changeImgBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                int result = chooser.showOpenDialog(AdminPanel.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File chosen = chooser.getSelectedFile();
                    String rel = copyFileToImages(chosen);
                    if (rel != null) {
                        newImagePath[0] = rel;
                        imgLabel.setText(rel);
                    } else {
                        JOptionPane.showMessageDialog(AdminPanel.this, "Failed to copy image.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });


        int option = JOptionPane.showConfirmDialog(
                this,
                form,
                "Edit Product",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option == JOptionPane.OK_OPTION) {
            String newName = nameField.getText().trim();
            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name cannot be empty");
                return;
            }

            Category newCategory = (Category) categoryBox.getSelectedItem();

            double newPrice;
            int newStock;
            try {
                newPrice = Double.parseDouble(priceField.getText().trim());
                newStock = Integer.parseInt(stockField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Price or stock is invalid");
                return;
            }

            String newDesc = descArea.getText().trim();


            product.setName(newName);
            product.setCategory(newCategory);
            product.setPrice(newPrice);
            product.setStock(newStock);
            product.setDescription(newDesc);
            product.setImagePath(newImagePath[0]);


            productService.saveToFile(repository);
            refreshTable();

            JOptionPane.showMessageDialog(this, "Product updated successfully");
        }
    }

}

class CustomerPanel extends JPanel {

    private ProductService productService;
    private ShoppingCart cart;
    private CartService cartService;
    private ProductFileRepository repository;
    private CartFileRepository cartRepository;

    private JTable productTable;
    private DefaultTableModel productTableModel;

    private JTable cartTable;
    private DefaultTableModel cartTableModel;

    private JLabel totalPriceLabel;

    private JTextField searchField;
    private JComboBox<String> categoryFilterBox;
    private JPanel searchPanel;

    private JButton addToCartButton;
    private JButton removeFromCartButton;
    private JButton checkoutButton;

    private JLabel headerImageLabel;
    private static final int HEADER_HEIGHT = 120;

    private JLabel imagePreview;
    private static final int PREVIEW_WIDTH = 180;
    private static final int PREVIEW_HEIGHT = 180;

    public CustomerPanel(ProductService productService, ProductFileRepository repository, CartFileRepository cartRepository) {
        this.productService = productService;
        this.repository = repository;
        this.cartRepository = cartRepository;
        this.cart = cartRepository.loadCart(productService);
        this.cartService = new CartService(this.cart, productService);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initProductTable();
        initCartPanelWithImage();
        initButtons();

        refreshProducts();
        refreshCart();

        productTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    updateImagePreviewForSelected();
                }
            }
        });

        if (productTable.getRowCount() > 0) {
            productTable.setRowSelectionInterval(0, 0);
            updateImagePreviewForSelected();
        }
    }

    private void initProductTable() {

        String[] columns = {
                "ID", "Name", "Category", "Price", "Stock"
        };

        productTableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        productTable = new JTable(productTableModel);

        searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));


        searchField = new JTextField(20);
        searchField.setToolTipText("Search by name");

        categoryFilterBox = new JComboBox<>();
        categoryFilterBox.addItem("All");
        for (Category c : Category.values()) {
            categoryFilterBox.addItem(c.name());
        }

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(new JLabel("Category:"));
        searchPanel.add(categoryFilterBox);


        add(new JScrollPane(productTable), BorderLayout.CENTER);

        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                updateProductFilter();
            }
        });

        categoryFilterBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateProductFilter();
            }
        });

        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }


    private void initCartPanelWithImage() {
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setPreferredSize(new Dimension(320, 0));


        imagePreview = new JLabel("", SwingConstants.CENTER);
        imagePreview.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        imagePreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        imagePreview.setHorizontalTextPosition(SwingConstants.CENTER);
        imagePreview.setVerticalTextPosition(SwingConstants.CENTER);
        imagePreview.setText("No Image");
        rightPanel.add(imagePreview, BorderLayout.NORTH);


        JPanel cartPanel = new JPanel(new BorderLayout(5, 5));

        cartTableModel = new DefaultTableModel(
                new String[]{"ID", "Name", "quantity", "Price"}, 0
        ) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        cartTable = new JTable(cartTableModel);

        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartPanel.add(cartScroll, BorderLayout.CENTER);

        totalPriceLabel = new JLabel("Total: 0.0");
        totalPriceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        cartPanel.add(totalPriceLabel, BorderLayout.SOUTH);

        rightPanel.add(cartPanel, BorderLayout.CENTER);

        add(rightPanel, BorderLayout.EAST);

        if (cartTable.getColumnModel().getColumnCount() > 0) {
            try {
                cartTable.removeColumn(cartTable.getColumnModel().getColumn(0));
            } catch (Exception ex) {
            }
        }
    }

    private void initButtons() {
        JPanel topPanel = new JPanel(new BorderLayout(8, 8));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        addToCartButton = new JButton("Add to Cart");
        removeFromCartButton = new JButton("Remove from Cart");
        checkoutButton = new JButton("Checkout");

        buttonPanel.add(addToCartButton);
        buttonPanel.add(removeFromCartButton);
        buttonPanel.add(checkoutButton);


        addToCartButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addToCart();
            }
        });

        removeFromCartButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeFromCart();
            }
        });

        checkoutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                checkout();
            }
        });

        if (searchPanel == null) {
            searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            searchField = new JTextField(20);
            searchPanel.add(new JLabel("Search:"));
            searchPanel.add(searchField);
            categoryFilterBox = new JComboBox<>();
            categoryFilterBox.addItem("All");
            for (Category c : Category.values()) categoryFilterBox.addItem(c.name());
            searchPanel.add(new JLabel("Category:"));
            searchPanel.add(categoryFilterBox);

            searchField.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyReleased(java.awt.event.KeyEvent e) {
                    updateProductFilter();
                }
            });
            categoryFilterBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateProductFilter();
                }
            });
        }

        topPanel.add(searchPanel, BorderLayout.WEST);

        topPanel.add(buttonPanel, BorderLayout.EAST);


        headerImageLabel = new JLabel("Welcome to the Store", SwingConstants.CENTER);
        headerImageLabel.setPreferredSize(new Dimension(800, HEADER_HEIGHT));
        headerImageLabel.setFont(new Font("Arial", Font.BOLD, 18));

        JPanel headerContainer = new JPanel(new BorderLayout());
        headerContainer.add(headerImageLabel, BorderLayout.NORTH);
        headerContainer.add(topPanel, BorderLayout.SOUTH);

        add(headerContainer, BorderLayout.NORTH);

        setHeaderImage("images/store_banner.jpg");
    }

    private void addToCart() {
        int row = productTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a product");
            return;
        }

        String productId = (String) productTableModel.getValueAt(row, 0);
        Product product = productService.findById(productId);

        if (product == null) {
            JOptionPane.showMessageDialog(this, "Product not found");
            return;
        }

        if (product.getStock() <= 0) {
            JOptionPane.showMessageDialog(this, "Out of stock");
            return;
        }

        cartService.addToCart(product, 1);
        cartRepository.saveCart(cart);

        refreshProducts();
        refreshCart();
    }

    private void updateProductFilter() {
        String txt = "";
        if (searchField != null && searchField.getText() != null) {
            txt = searchField.getText().trim().toLowerCase();
        }

        String categorySelected = null;
        if (categoryFilterBox != null && categoryFilterBox.getSelectedItem() != null) {
            categorySelected = ((String) categoryFilterBox.getSelectedItem());
        }

        productTableModel.setRowCount(0);

        for (Product p : productService.getAllProducts()) {
            if (categorySelected != null && !"All".equals(categorySelected)) {
                if (!p.getCategory().name().equals(categorySelected)) {
                    continue;
                }
            }

            if (!txt.isEmpty()) {
                String nameLower = p.getName() == null ? "" : p.getName().toLowerCase();
                if (!nameLower.contains(txt)) {
                    continue;
                }
            }

            productTableModel.addRow(new Object[]{
                    p.getId(),
                    p.getName(),
                    p.getCategory(),
                    p.getPrice(),
                    p.getStock()
            });
        }

        if (productTable.getRowCount() > 0) {
            productTable.setRowSelectionInterval(0, 0);
            updateImagePreviewForSelected();
        } else {
            showNoImage();
        }
    }

    private void removeFromCart() {
        int viewRow = cartTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "To remove an item, select it from the cart table");
            return;
        }

        String productId = (String) cartTableModel.getValueAt(viewRow, 0);

        for (CartItem item : cart.getItems()) {
            if (item.getProduct().getId().equals(productId)) {
                cart.decreaseProduct(item.getProduct(), 1);
                cartRepository.saveCart(cart);
                break;
            }
        }

        refreshProducts();
        refreshCart();
    }

    private void checkout() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Confirm purchase?",
                "Checkout",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = cartService.checkout(repository);
            if (success) {
                refreshProducts();
                refreshCart();
                cartRepository.saveCart(cart);
                JOptionPane.showMessageDialog(this, "Purchase successful!");
            } else {
                JOptionPane.showMessageDialog(this, "Purchase failed: insufficient stock for some items.");
            }
        }
    }

    private void refreshProducts() {
        productTableModel.setRowCount(0);

        for (Product p : productService.getAllProducts()) {
            productTableModel.addRow(new Object[]{
                    p.getId(),
                    p.getName(),
                    p.getCategory(),
                    p.getPrice(),
                    p.getStock()
            });
        }

        if (productTable.getRowCount() > 0) {
            productTable.setRowSelectionInterval(0, 0);
            updateImagePreviewForSelected();
        } else {
            showNoImage();
        }
    }

    private void refreshCart() {
        cartTableModel.setRowCount(0);

        for (CartItem item : cart.getItems()) {
            Product p = item.getProduct();
            cartTableModel.addRow(new Object[]{
                    p.getId(),
                    p.getName(),
                    item.getQuantity(),
                    item.getTotalPrice()
            });
        }

        totalPriceLabel.setText(
                "Total: " + cart.getTotalPrice()
        );
    }


    private void updateImagePreviewForSelected() {
        int row = productTable.getSelectedRow();
        if (row == -1) {
            showNoImage();
            return;
        }

        String productId = (String) productTableModel.getValueAt(row, 0);
        Product p = productService.findById(productId);
        if (p == null) {
            showNoImage();
            return;
        }

        String path = p.getImagePath();
        if (path == null || path.trim().isEmpty()) {
            showNoImage();
            return;
        }

        ImageIcon icon = createScaledImageIcon(path, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        if (icon != null) {
            imagePreview.setText("");
            imagePreview.setIcon(icon);
        } else {
            showNoImage();
        }
    }

    private void showNoImage() {
        imagePreview.setIcon(null);
        imagePreview.setText("No Image");
    }

    private ImageIcon createScaledImageIcon(String path, int w, int h) {
        try {
            File f = new File(path);
            if (!f.exists()) return null;

            ImageIcon original = new ImageIcon(path);
            Image img = original.getImage();
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }

    private void setHeaderImage(String path) {
        ImageIcon icon = createScaledImageIcon(path, 700, HEADER_HEIGHT);
        if (icon != null) {
            headerImageLabel.setText("");
            headerImageLabel.setIcon(icon);
        } else {
            headerImageLabel.setIcon(null);
            headerImageLabel.setText("Welcome to the Store");
        }
    }

    public ShoppingCart getShoppingCart() {
        return this.cart;
    }
}
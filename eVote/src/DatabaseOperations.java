import java.sql.*;

public class DatabaseOperations {
    private Connection connectUsers;
    private Connection connectSignatures;

    public DatabaseOperations() {
        connectDatabaseSignatures();
        connectToDatabaseUsers();
        String createUsersTable = "CREATE TABLE IF NOT EXISTS Users (\n"
                + " id integer primary key,\n"
                + " name text not null,\n"
                + " surname text not null,\n"
                + " email text not null,\n"
                + " SSN text not null\n"
                + ");";

        String createUserChecksumTable = "CREATE TABLE IF NOT EXISTS UserChecksums (\n"
                + " id integer primary key,\n"
                + " SSN text not null\n"
                + ");";

        String createSignaturesTable = "CREATE TABLE IF NOT EXISTS Signatures(\n"
                + " id integer primary key,\n"
                + " signature text not null,\n"
                + " isVoted integer not null\n"
                + ");";

        Statement statementUsers;
        Statement statementCertificates;
        try {
            statementCertificates = connectSignatures.createStatement();
            statementCertificates.execute(createSignaturesTable);
            statementUsers = connectUsers.createStatement();
            statementUsers.execute(createUsersTable);
            statementUsers.execute(createUserChecksumTable);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnectionSignatures();
            closeConnectionUsers();
        }
    }


    public void closeConnectionUsers() {
        try {
            connectUsers.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void closeConnectionSignatures() {
        try {
            connectSignatures.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public boolean connectToDatabaseUsers() {
        try {
            String connectionUrl = "jdbc:sqlite:users.db";
            connectUsers = DriverManager.getConnection(connectionUrl);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

    }

    public boolean connectDatabaseSignatures() {
        try {
            String connectionUrl = "jdbc:sqlite:signatures.db";
            connectSignatures = DriverManager.getConnection(connectionUrl);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

    }

    public void updateVoter(String signature) {
        String sql = "UPDATE Signatures SET isVoted = 1 WHERE signature = '"+signature+"'";
        try {
            PreparedStatement preparedStatement = connectSignatures.prepareStatement(sql);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isVotedBefore(String signature) {
        String sql = "SELECT isVoted FROM Signatures WHERE signature = '"+signature+"'";
        try {
            Statement statement = connectSignatures.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            int isVoted=2;
            while(resultSet.next()) {
                isVoted = resultSet.getInt("isVoted");
            }
            if(isVoted==1) {
                return true;
            }
            else if(isVoted==0) {
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void insertUser(String SSN) {
        String sql = "INSERT INTO UserChecksums(SSN) VALUES(?)";

        try {
            PreparedStatement pstmt = connectUsers.prepareStatement(sql);
            pstmt.setString(1, SSN);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void insertSignature(String signature) {
        String sql = "INSERT INTO Signatures(signature,isVoted) VALUES(?,?)";

        try {
            PreparedStatement pstmt = connectSignatures.prepareStatement(sql);
            pstmt.setString(1, signature);
            pstmt.setInt(2, 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public String getEncUserPassword(String userMail) {
        String query = "SELECT password FROM Users WHERE email='" + userMail + "'";
        String returnedEncPassword = "";
        try {
            Statement statement = connectUsers.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                returnedEncPassword = resultSet.getString("password");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            return returnedEncPassword;
        }
    }

    public boolean signatureCheck(String signature) {
        String getQuery = "SELECT * FROM Signatures WHERE signature='" + signature + "'";

        try {
            Statement statement = connectSignatures.createStatement();
            ResultSet resultSet = statement.executeQuery(getQuery);
            if (resultSet.next()) {
                return true;
            } else return false; // User non existant

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
        return false;
    }



    public boolean userCheck(String ssn, String name, String sname, String email) {
        String getQuery = "SELECT * FROM Users WHERE SSN='" + ssn + "'AND name='" + name + "'AND surname='" + sname + "'AND email='" + email + "'";

        try {
            Statement statement = connectUsers.createStatement();
            ResultSet resultSet = statement.executeQuery(getQuery);
            if (resultSet.next()) {
                return true;
            } else return false; // User non existant

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
        return false;
    }

    public boolean userCheckSum(String ssn) {
        String getQuery = "SELECT * FROM UserChecksums WHERE SSN='" + ssn + "'";
        try {
            Statement statement = connectUsers.createStatement();
            ResultSet resultSet = statement.executeQuery(getQuery);
            if (resultSet.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
        return false;

    }
}





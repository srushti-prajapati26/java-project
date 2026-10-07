import java.util.Scanner;
class BankAccount
{
	private String accountNumber;
	private String accountholder;
	private double balance;
	
	public BankAccount(String accountNumber,String accountholder,double initialBalance)
	{
		this.accountNumber = accountNumber;
		this.accountholder = accountholder;
		if(initialBalance >= 0)
		{
			this.balance= initialBalance;
		}//if
		else
		{
			this.balance = 0.0;
			System.out.println("Warning: Initial deposit cannot be negative.");

			
		}//else
	}//public_ba()
	public double getBalance()
	{
		return balance;
	}//get_bal()
	public void deposit(double amount)
	{
		if(amount>0)
		{
			balance += amount;
			System.out.println("Succesfully deposited: "+amount);
			System.out.println("Updated Balance: " +balance);
 
		}//if
		else
		{
			System.out.println("Error: Deposit amount must be positive. ");
		}//else

	}//public_deposit()
	
	public void withdraw(double amount)
	{
	if(amount<=0)
	{
		System.out.println("Error: Withdrawal amount must be positive. ");

	}//if
	else if(amount>balance)
	{
		System.out.println("Error: Insufficient funds! Current Balance:"+balance);

	}//else if

	else
	{
		balance-= amount;
		System.out.println("Successfully withdraw:"+amount);
		System.out.println("Remaining Balance:"+balance);
	

	}//else
}//public_wd

public void displayDetails()
{
	System.out.println("\n---------------------------------");
	System.out.println("Account Number: " +accountNumber);
	System.out.println("Account Holder: " +accountholder);
	System.out.println("Current Balance: "+balance);
	System.out.println("-----------------------------------");
}//public_dis_details
}//class_ba


public class BankSystem
{
	public static void main(String[] args)
	{
		Scanner scanner = new Scanner(System.in);
	
	System.out.println("================================================");
	System.out.println("WELCOME TO TSDCEM BANKING SYSTEM  ");
	System.out.println("================================================");
	
	System.out.println("Enter Account Number: ");
	String accNum =scanner.nextLine();

	System.out.println("Enter Account Holder Name: ");
	String holderName =scanner.nextLine();

	System.out.println("Enter Initial Deposit: ");
	double initialDeposit =scanner.nextDouble();

	BankAccount account = new BankAccount(accNum,holderName,initialDeposit);
	
	System.out.println("\n Account Created Successfully!!");

	Boolean isRunning = true;
	while(isRunning)
	{
		System.out.println("\n=======MAIN MENU=======");
		System.out.println("1.Deposit Money");
		System.out.println("2.Withdraw Money");
		System.out.println("3.Check Balence & Details");
		System.out.println("4.Exit");
		System.out.println("Enter your choice(1-4):");
		
		int choice =scanner.nextInt();

		switch(choice)
		{

		case 1:
		System.out.println("Enter amount to deposit: ");
		double depAmt =scanner.nextDouble();
		account.deposit(depAmt);
		break;

		case 2:
		System.out.println("Enter amount to withdraw: ");
		double withAmt =scanner.nextDouble();
		account.withdraw(withAmt);
		break;

		case 3:
		account.displayDetails();
		break;
		
		case 4:
		System.out.println("\n Thank you for using TSDCEM Bank System. Goodbye!");
		isRunning =false;
		break;

		default:
		System.out.println("Error:Invaild Choice! Select between 1 to 4.");
		}//switch
	}//while
	
	}//void main()

}//public_bs












	

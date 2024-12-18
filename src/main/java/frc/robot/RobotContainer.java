// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import frc.robot.Constants.OperatorConstants;
import frc.robot.Constants.ShooterConstants;
import frc.robot.Constants.AutonomousConstants;
import frc.robot.Constants.LEDConstants;

import frc.robot.commands.DriveCommand;
import frc.robot.commands.DropIntakeCommand;
import frc.robot.commands.IntakeCommand;
import frc.robot.commands.LaunchBallCommand;
import frc.robot.commands.ReverseIntakeCommand;
import frc.robot.commands.ShooterIntakeCommand;
import frc.robot.commands.RevUpShooterPercentCommand;
import frc.robot.subsystems.ClimbSubsystem;
import frc.robot.subsystems.DriveSubsystem;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;

import frc.robot.subsystems.IndexSubsystem;
import frc.robot.subsystems.IntakeSubsystem;
import frc.robot.subsystems.ShooterSubsystem;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;

import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.IntegerPublisher;
import edu.wpi.first.networktables.NetworkTable;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of the robot (including
 * subsystems, commands, and trigger mappings) should be declared here.
 */
public class RobotContainer {
  // The robot's subsystems and commands are defined here...
  private final DriveSubsystem m_driveSubsystem = new DriveSubsystem();
  private final IntakeSubsystem m_intakeSubsystem = new IntakeSubsystem();
  private final ShooterSubsystem m_shooterSubsystem = new ShooterSubsystem();
  private final IndexSubsystem m_indexSubsystem = new IndexSubsystem();
  // private final ClimbSubsystem m_climbSubsystem = new ClimbSubsystem();

  // Controllers
  private final CommandXboxController m_driverController = new CommandXboxController(OperatorConstants.kDriverControllerPort);
  private final CommandXboxController m_coDriverController = new CommandXboxController(OperatorConstants.kCoDriverControllerPort);

  private final SendableChooser<Command> m_autonChooser;

  private IntegerPublisher m_LEDIndexPublisher;

  /** The container for the robot. Contains subsystems, OI devices, and commands. */
  public RobotContainer() {

    // Connect to camera
    CameraServer.startAutomaticCapture();

    // Configure network tables to communicate with LEDs
    configureNetworkTables();

    // Configure the controller bindings
    configureBindings();

    // Register commands to pathplanner
    registerCommands();

    m_autonChooser = AutoBuilder.buildAutoChooser();

    Shuffleboard.getTab("Auton").add("Auton Selector", m_autonChooser);

    Shuffleboard.getTab("Auton").addDouble("Match Timer", () -> DriverStation.isTeleopEnabled() ? DriverStation.getMatchTime() : -1);
  }

  /** 
   * This method maps controller inputs to commands. 
   * This is handled in a separate function to keep things organized. 
   * */
  private void configureBindings() {
    // Configures robot to drive with joystick inputs by default
    m_driveSubsystem.setDefaultCommand(
      new DriveCommand(m_driveSubsystem, m_driverController::getLeftY, m_driverController::getRightX, () -> m_driverController.rightBumper().getAsBoolean())
    );

    // Configures intake to start when the b button is held on the Co-Driver Controller.
    m_coDriverController.b().whileTrue(new IntakeCommand(m_intakeSubsystem, m_indexSubsystem));

    // Configures the intake to reverse when the x button is held on the Co-Driver Controller.
    m_coDriverController.x().whileTrue(new ReverseIntakeCommand(m_intakeSubsystem, m_indexSubsystem));

    // Configures the shooter intake to start when the a button is held on the Co-Driver Controller.
    m_coDriverController.a().whileTrue(new ShooterIntakeCommand(m_shooterSubsystem, m_indexSubsystem));

    // Configures the shooter to rev up when the left trigger is held on the Co-Driver Controller.
    // The speed is controled by the analog input of the trigger.
    //m_coDriverController.leftTrigger(0.1).whileTrue(new RevUpShooterCommand(m_shooterSubsystem, m_coDriverController::getLeftTriggerAxis));
    m_coDriverController.leftTrigger(0.1)
      .whileTrue(new RevUpShooterPercentCommand(m_shooterSubsystem, ShooterConstants.kTopShooterSpeed, ShooterConstants.kBottomShooterSpeed));

    // Configures the ball to launch when the right trigger is pressed.
    m_coDriverController.rightTrigger(0.3).whileTrue(new LaunchBallCommand(m_indexSubsystem));

    m_coDriverController.povUp().onTrue(new InstantCommand(() -> {
      m_LEDIndexPublisher.set(LEDConstants.Feedme);
    }));

    m_coDriverController.povDown().onTrue(new InstantCommand(() -> {
      m_LEDIndexPublisher.set(LEDConstants.Yipee);
    }));

    m_coDriverController.povLeft().onTrue(new InstantCommand(() -> {
      m_LEDIndexPublisher.set(LEDConstants.Boykisser);
    }));

    // m_coDriverController.povRight().onTrue(new InstantCommand(() -> {
      
    // }));
  }

  /**
   * This method configures NT tables used to communicate with the LEDs.
   * This is handled in a separate function to keep things organized.
   */
  private void configureNetworkTables() {
    NetworkTableInstance networkInstance = NetworkTableInstance.getDefault();
    NetworkTable table = networkInstance.getTable(LEDConstants.LEDTableName);

    m_LEDIndexPublisher = table.getIntegerTopic(LEDConstants.LEDIndexName).publish();
  }

  /**
   * This method registers autonomous commands so that they can be used in pathplanner.
   * This is handled in a separate function to keep things organized.
   */
  private void registerCommands() {
    NamedCommands.registerCommand("Rev Up Shooter", 
      new RevUpShooterPercentCommand(m_shooterSubsystem, ShooterConstants.kTopShooterSpeed, ShooterConstants.kBottomShooterSpeed)
    );
    NamedCommands.registerCommand("Shoot", new LaunchBallCommand(m_indexSubsystem).withTimeout(AutonomousConstants.kBallLaunchTimeout));
    NamedCommands.registerCommand("Intake", new IntakeCommand(m_intakeSubsystem, m_indexSubsystem).withTimeout(3));
    NamedCommands.registerCommand("Drop Intake", new DropIntakeCommand(m_driveSubsystem));
  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand() {
    // Run the command currently selected on shuffleboard
    return m_autonChooser.getSelected();
  }

  /** Set the drivetrain to brake mode when enabled. */
  public void setBrakeMode() {
    m_driveSubsystem.setBrakeMode();
  }

  /** Set the drivetrain to coast mode when disabled. */
  public void setCoastMode() {
    m_driveSubsystem.setCoastMode();
  }

  /** Stop drivetrain movement at the end of auton. */
  public void stopDrivetrain() {
    m_driveSubsystem.stopDrive();
  }
}
// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.controls.NeutralOut;
import com.ctre.phoenix6.controls.VelocityTorqueCurrentFOC;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.hardware.TalonFX;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.sim.PhysicsSim;
import edu.wpi.first.networktables.GenericEntry;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the
 * name of this class or
 * the package after creating this project, you must also update the
 * build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
  private static final String canBusName = "canivore";
  private final TalonFX m_fxTop = new TalonFX(6);
  private final TalonFX m_fxBottom = new TalonFX(5);
  private final TalonFX m_fxIndexTL = new TalonFX(7);
  private final TalonFX m_fxIndexTR = new TalonFX(8);
  private final TalonFX m_fxIntakeTop = new TalonFX(9);
  private final TalonFX m_fxInakteBottom = new TalonFX(10);
  private GenericEntry m_ShooterBottomVelocity = null;
  private GenericEntry m_ShooterTopVelocity = null;

  /* Be able to switch which control request to use based on a button press */
  /* Start at velocity 0, enable FOC, no feed forward, use slot 0 */
  private final VelocityVoltage m_voltageVelocity = new VelocityVoltage(0, 0, true, 0, 0, false, false, false);
  /* Start at velocity 0, no feed forward, use slot 1 */
  private final VelocityTorqueCurrentFOC m_torqueVelocity = new VelocityTorqueCurrentFOC(0, 0, 0, 1, false, false,
      false);
  /* Keep a neutral out so we can disable the motor */
  private final NeutralOut m_brake = new NeutralOut();

  private final XboxController m_joystick = new XboxController(0);

  private final Mechanisms m_mechanisms = new Mechanisms();

  /**
   * This function is run when the robot is first started up and should be used
   * for any
   * initialization code.
   */
  @Override
  public void robotInit() {
    // amp: 9.5 Bottom, 9.5 Top

    m_ShooterTopVelocity = Shuffleboard.getTab("Shooter")
        .add("ShooterTop", -9.5).getEntry();

    m_ShooterBottomVelocity = Shuffleboard.getTab("Shooter")
        .add("ShooterBottom", -9.5).getEntry();

    TalonFXConfiguration configs = new TalonFXConfiguration();

    /*
     * Voltage-based velocity requires a feed forward to account for the back-emf of
     * the motor
     */
    configs.Slot0.kP = 0.11; // An error of 1 rotation per second results in 2V output
    configs.Slot0.kI = 0.5; // An error of 1 rotation per second increases output by 0.5V every second
    configs.Slot0.kD = 0.0001; // A change of 1 rotation per second squared results in 0.01 volts output
    configs.Slot0.kV = 0.12; // Falcon 500 is a 500kV motor, 500rpm per V = 8.333 rps per V, 1/8.33 = 0.12
                             // volts / Rotation per second
    // Peak output of 8 volts
    configs.Voltage.PeakForwardVoltage = 8;
    configs.Voltage.PeakReverseVoltage = -8;

    /*
     * Torque-based velocity does not require a feed forward, as torque will
     * accelerate the rotor up to the desired velocity by itself
     */
    configs.Slot1.kP = 5; // An error of 1 rotation per second results in 5 amps output
    configs.Slot1.kI = 0.1; // An error of 1 rotation per second increases output by 0.1 amps every second
    configs.Slot1.kD = 0.001; // A change of 1000 rotation per second squared results in 1 amp output

    // Peak output of 40 amps
    configs.TorqueCurrent.PeakForwardTorqueCurrent = 40;
    configs.TorqueCurrent.PeakReverseTorqueCurrent = -40;

    /* Retry config apply up to 5 times, report if failure */
    StatusCode status = StatusCode.StatusCodeNotInitialized;
    for (int i = 0; i < 5; ++i) {
      status = m_fxTop.getConfigurator().apply(configs);
      if (status.isOK())
        break;
    }
    if (!status.isOK()) {
      System.out.println("Could not apply configs, error code: " + status.toString());
    }

    for (int i = 0; i < 5; ++i) {
      status = m_fxBottom.getConfigurator().apply(configs);
      if (status.isOK())
        break;
    }
    if (!status.isOK()) {
      System.out.println("Could not apply configs, error code: " + status.toString());
    }

    for (int i = 0; i < 5; ++i) {
      status = m_fxIndexTL.getConfigurator().apply(configs);
      if (status.isOK())
        break;
    }
    if (!status.isOK()) {
      System.out.println("Could not apply configs, error code: " + status.toString());
    }

    for (int i = 0; i < 5; ++i) {
      status = m_fxIndexTR.getConfigurator().apply(configs);
      if (status.isOK())
        break;
    }
    if (!status.isOK()) {
      System.out.println("Could not apply configs, error code: " + status.toString());
    }

    m_fxTop.setInverted(true);
    // m_fxBottom.setControl(new Follower(m_fxTop.getDeviceID(), false));
  }

  @Override
  public void robotPeriodic() {
    m_mechanisms.update(m_fxTop.getPosition(), m_fxTop.getVelocity());
  }

  @Override
  public void autonomousInit() {
  }

  @Override
  public void autonomousPeriodic() {
  }

  @Override
  public void teleopInit() {
  }

  @Override
  public void teleopPeriodic() {
    SmartDashboard.putNumber("shooterTopVel", m_fxTop.getVelocity().getValueAsDouble());
    SmartDashboard.putNumber("shooterBottomVel", m_fxBottom.getVelocity().getValueAsDouble());

    double joyValue = m_joystick.getLeftY();
    if (joyValue > -0.1 && joyValue < 0.1)
      joyValue = 0;

    double desiredTopRPS = m_ShooterTopVelocity.getDouble(0);
    double desiredBottomRPS = m_ShooterBottomVelocity.getDouble(0);

    if (m_joystick.getLeftBumper()) {
      /* Use voltage velocity */
      m_fxTop.setControl(m_voltageVelocity.withVelocity(desiredTopRPS));
      m_fxBottom.setControl(m_voltageVelocity.withVelocity(desiredBottomRPS));
    } else if (m_joystick.getRightBumper()) {
      double friction_torque = (joyValue > 0) ? 1 : -1; // To account for friction, we add this to the arbitrary feed
                                                        // forward
      /* Use torque velocity */
      m_fxTop.setControl(m_torqueVelocity.withVelocity(desiredTopRPS).withFeedForward(friction_torque));
      m_fxBottom.setControl(m_torqueVelocity.withVelocity(desiredBottomRPS).withFeedForward(friction_torque));
    } else {
      /* Disable the motor instead */
      m_fxTop.setControl(m_brake);
      m_fxBottom.setControl(m_brake);
    }

    if (m_joystick.getAButton()) {
      m_fxIndexTL.setControl(m_voltageVelocity.withVelocity(-5.0));
      m_fxIndexTR.setControl(m_voltageVelocity.withVelocity(5.0));
      m_fxIntakeTop.setControl(m_voltageVelocity.withVelocity(-45.0));
      m_fxInakteBottom.setControl(m_voltageVelocity.withVelocity(-45.0));
    } else if (m_joystick.getBButton()) {
      m_fxIntakeTop.setControl(m_voltageVelocity.withVelocity(45.0));
      m_fxInakteBottom.setControl(m_voltageVelocity.withVelocity(45.0));
      m_fxIndexTL.setControl(m_voltageVelocity.withVelocity(3.0));
      m_fxIndexTR.setControl(m_voltageVelocity.withVelocity(-3.0));
      m_fxTop.setControl(m_voltageVelocity.withVelocity(-3));
      m_fxBottom.setControl(m_voltageVelocity.withVelocity(-3));
    } else {
      m_fxIntakeTop.setControl(m_brake);
      m_fxInakteBottom.setControl(m_brake);
      m_fxIndexTL.setControl(m_brake);
      m_fxIndexTR.setControl(m_brake);
    }

  }

  @Override
  public void disabledInit() {
  }

  @Override
  public void disabledPeriodic() {
  }

  @Override
  public void testInit() {
  }

  @Override
  public void testPeriodic() {
  }

  @Override
  public void simulationInit() {
    PhysicsSim.getInstance().addTalonFX(m_fxTop, 0.001);
  }

  @Override
  public void simulationPeriodic() {
    PhysicsSim.getInstance().run();
  }
}
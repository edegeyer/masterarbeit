package com.loomo.robot.mycroftcontrol.interfaces;

import com.segway.robot.algo.dts.DTSPerson;

/**
 * @author jacob
 * @date 5/29/18
 */

public interface PresenterChangeInterface {

    void dismissLoading();

    void showToast(String message);

    void drawPersons(DTSPerson[] dtsPersons);

    void drawPerson(DTSPerson dtsPerson);
}

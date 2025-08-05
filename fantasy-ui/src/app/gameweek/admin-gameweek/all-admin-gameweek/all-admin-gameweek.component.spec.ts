import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AllAdminGameweekComponent } from './all-admin-gameweek.component';

describe('AllAdminGameweekComponent', () => {
  let component: AllAdminGameweekComponent;
  let fixture: ComponentFixture<AllAdminGameweekComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AllAdminGameweekComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AllAdminGameweekComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

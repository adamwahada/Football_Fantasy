import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserGameweekClassementComponent } from './user-gameweek-classement.component';

describe('UserGameweekClassementComponent', () => {
  let component: UserGameweekClassementComponent;
  let fixture: ComponentFixture<UserGameweekClassementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserGameweekClassementComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserGameweekClassementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
